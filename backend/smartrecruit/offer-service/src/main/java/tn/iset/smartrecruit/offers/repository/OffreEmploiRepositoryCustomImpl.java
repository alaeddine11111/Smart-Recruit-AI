package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.dto.OffreSkillsView;
import tn.iset.smartrecruit.offers.dto.SkillCount;
import tn.iset.smartrecruit.offers.dto.OffreFilter;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import tn.iset.smartrecruit.offers.model.OfferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/** Spring Data picks this up automatically: <FragmentInterface>Impl. */
@RequiredArgsConstructor
public class OffreEmploiRepositoryCustomImpl implements OffreEmploiRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public Page<OffreEmploi> search(OffreFilter f, Pageable pageable) {
        List<Criteria> c = new ArrayList<>();
        if (f.statut() != null)       c.add(Criteria.where("statut").is(f.statut()));
        if (f.recruteurId() != null)  c.add(Criteria.where("recruteurId").is(f.recruteurId()));
        if (f.typeContrat() != null)  c.add(Criteria.where("typeContrat").is(f.typeContrat()));
        if (notBlank(f.titre()))
            c.add(Criteria.where("titre").regex(Pattern.compile(Pattern.quote(f.titre().trim()), Pattern.CASE_INSENSITIVE)));
        if (notBlank(f.localisation()))
            c.add(Criteria.where("localisation").regex(Pattern.compile(Pattern.quote(f.localisation().trim()), Pattern.CASE_INSENSITIVE)));
        if (f.competences() != null && !f.competences().isEmpty()) {
            List<String> skills = f.competences().stream().filter(this::notBlank)
                    .map(s -> s.trim().toLowerCase()).toList();
            if (!skills.isEmpty()) c.add(Criteria.where("competences").all(skills));
        }

        Query q = new Query();
        if (!c.isEmpty()) q.addCriteria(new Criteria().andOperator(c.toArray(new Criteria[0])));

        long total = mongo.count(q, OffreEmploi.class);
        List<OffreEmploi> rows = mongo.find(q.with(pageable), OffreEmploi.class);
        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public List<SkillCount> aggregatePublishedSkills(String query, int limit) {
        List<AggregationOperation> stages = new ArrayList<>();
        stages.add(match(Criteria.where("statut").is(OfferStatus.PUBLISHED)));
        stages.add(unwind("competences"));
        stages.add(group("competences").count().as("count"));
        if (notBlank(query)) {
            stages.add(match(Criteria.where("_id").regex(
                    Pattern.compile(Pattern.quote(query.trim()), Pattern.CASE_INSENSITIVE))));
        }
        stages.add(sort(Sort.by(Sort.Order.desc("count"), Sort.Order.asc("_id"))));
        stages.add(limit(limit));
        stages.add(project("count").and("_id").as("competence").andExclude("_id"));

        AggregationResults<SkillCount> results = mongo.aggregate(
                newAggregation(stages), OffreEmploi.class, SkillCount.class);
        return results.getMappedResults();
    }

    @Override
    public List<OffreSkillsView> publishedSkillOffers(int limit) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("statut").is(OfferStatus.PUBLISHED)),
                project("titre", "entrepriseNom", "localisation", "typeContrat", "competences")
                        .and("_id").as("id"),
                limit(limit));
        return mongo.aggregate(aggregation, OffreEmploi.class, OffreSkillsView.class)
                .getMappedResults();
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
