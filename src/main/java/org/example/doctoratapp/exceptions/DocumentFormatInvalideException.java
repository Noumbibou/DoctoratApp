package org.example.doctoratapp.exceptions;

public class DocumentFormatInvalideException extends RuntimeException {

    public DocumentFormatInvalideException(String format) {
        super("Format de document non autorisé : " + format +
                ". Formats acceptés : PDF, JPG, JPEG, PNG");
    }
}
