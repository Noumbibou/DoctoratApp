package org.example.doctoratapp.exceptions;

import java.util.List;

// Lancée quand un doctorant soumet une demande sans remplir les prérequis
public class PrerequísNonRemplisException extends RuntimeException {

    private final List<String> prerequisManquants;

    public PrerequísNonRemplisException(List<String> prerequisManquants) {
        super("Prérequis non remplis pour la soutenance");
        this.prerequisManquants = prerequisManquants;
    }

    public List<String> getPrerequísManquants() {
        return prerequisManquants;
    }
}
