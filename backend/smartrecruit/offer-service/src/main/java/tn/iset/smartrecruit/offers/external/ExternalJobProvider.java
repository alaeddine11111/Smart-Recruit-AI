package tn.iset.smartrecruit.offers.external;

import java.util.List;

public interface ExternalJobProvider {
    String name();
    List<ExternalOffre> search(String keywords, String location, int page);
}

