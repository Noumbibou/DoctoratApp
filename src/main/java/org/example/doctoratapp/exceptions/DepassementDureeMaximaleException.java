package org.example.doctoratapp.exceptions;

// Lancée quand un doctorant dépasse 6 ans
public class DepassementDureeMaximaleException extends RuntimeException {

    public DepassementDureeMaximaleException(String nomDoctorant) {
        super("Le doctorant " + nomDoctorant + " a dépassé la durée maximale de 6 ans");
    }
}
