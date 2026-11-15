package tn.iset.smartrecruit.offers.external;

/**
 * titre / description are in English when the translation is active.
 * titreOriginal / descriptionOriginal / langueSource are only filled for translated offers.
 */
public record ExternalOffre(String id, String titre, String entreprise, String localisation,
                            String description, String salaire, String typeContrat,
                            String dateMiseAJour, String urlExterne, String source,
                            String titreOriginal, String descriptionOriginal, String langueSource) {

    /** Keeps the old 10-argument constructor so the providers do not need any change. */
    public ExternalOffre(String id, String titre, String entreprise, String localisation,
                         String description, String salaire, String typeContrat,
                         String dateMiseAJour, String urlExterne, String source) {
        this(id, titre, entreprise, localisation, description, salaire, typeContrat,
                dateMiseAJour, urlExterne, source, null, null, null);
    }

    public ExternalOffre withTranslation(String newTitre, String newDescription, String lang) {
        return new ExternalOffre(id, blankOr(newTitre, titre), entreprise, localisation,
                blankOr(newDescription, description), salaire, typeContrat, dateMiseAJour,
                urlExterne, source, titre, description, lang);
    }

    private static String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
