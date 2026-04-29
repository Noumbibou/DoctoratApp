package org.example.doctoratapp.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Exceptions métier ────────────────────────────────────

    // 404 — Resource non trouvée
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(404, ex.getMessage());
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(404, ex.getMessage());
        return ResponseEntity.status(404).body(error);
    }

    // 400 — Erreur métier (mauvaise requête)
    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ResponseEntity<ErrorResponse> handleEmailDejaUtilise(EmailDejaUtiliseException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(ReInscriptionNonAutoriseeException.class)
    public ResponseEntity<ErrorResponse> handleReInscription(ReInscriptionNonAutoriseeException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(DepassementDureeMaximaleException.class)
    public ResponseEntity<ErrorResponse> handleDepassement(DepassementDureeMaximaleException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(CampagneFermeeException.class)
    public ResponseEntity<ErrorResponse> handleCampagneFermee(CampagneFermeeException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(SoutenanceDejaAutoriseeException.class)
    public ResponseEntity<ErrorResponse> handleSoutenanceDejaAutorisee(SoutenanceDejaAutoriseeException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    @ExceptionHandler(DocumentFormatInvalideException.class)
    public ResponseEntity<ErrorResponse> handleDocumentFormat(DocumentFormatInvalideException ex) {
        ErrorResponse error = new ErrorResponse(400, ex.getMessage());
        return ResponseEntity.status(400).body(error);
    }

    // Prérequis non remplis — retourne la liste des manquants
    @ExceptionHandler(PrerequísNonRemplisException.class)
    public ResponseEntity<ErrorResponse> handlePrerequis(PrerequísNonRemplisException ex) {
        ErrorResponse error = new ErrorResponse(
                400,
                ex.getMessage(),
                LocalDateTime.now(),
                ex.getPrerequísManquants() // ← liste détaillée des manquants
        );
        return ResponseEntity.status(400).body(error);
    }

    // ── Validation @Valid ─────────────────────────────────────
    // Lancée quand les annotations @NotBlank, @Email... échouent
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        // Récupérer tous les messages d'erreur de validation
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                400,
                "Erreur de validation",
                LocalDateTime.now(),
                details
        );
        return ResponseEntity.status(400).body(error);
    }

    // ── Exception générique ──────────────────────────────────
    // Capture tout ce qui n'est pas géré au-dessus
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(500, "Erreur interne du serveur");
        return ResponseEntity.status(500).body(error);
    }
}
