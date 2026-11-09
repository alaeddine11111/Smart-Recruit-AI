package tn.iset.smartrecruit.offers.repository;

import tn.iset.smartrecruit.offers.dto.OffreFilter;
import tn.iset.smartrecruit.offers.model.OffreEmploi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}

