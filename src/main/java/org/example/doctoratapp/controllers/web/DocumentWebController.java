package org.example.doctoratapp.controllers.web;

import org.example.doctoratapp.entities.Document;
import org.example.doctoratapp.services.interfaces.IDocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/documents")
public class DocumentWebController {

    private final IDocumentService documentService;

    public DocumentWebController(IDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{id}/telecharger")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Long id) {
        Document document = documentService.findById(id);

        if (document.getCheminFichier() != null) {
            java.io.File file = new java.io.File(document.getCheminFichier());
            if (file.exists() && file.isFile()) {
                Resource resource = new org.springframework.core.io.FileSystemResource(file);
                String contentType = document.getFormat() != null ? document.getFormat() : "application/octet-stream";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getNomFichier() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(file.length())
                        .body(resource);
            }
        }

        // Fichier physique non existant (par ex: données de simulation ou créées avant l'activation du stockage physique)
        // On génère un simulacre respectant le format original (sans forcer l'extension .txt)
        String nomFichier = document.getNomFichier();
        String format = document.getFormat() != null ? document.getFormat().toLowerCase() : "";
        
        if (nomFichier.toLowerCase().endsWith(".pdf") || format.contains("pdf")) {
            // Un PDF minimal valide de 1-page pour éviter que le lecteur PDF ne signale un fichier corrompu
            byte[] miniPdf = ("%PDF-1.4\n" +
                    "1 0 obj <</Type/Catalog/Pages 2 0 R>> endobj\n" +
                    "2 0 obj <</Type/Pages/Kids[3 0 R]/Count 1>> endobj\n" +
                    "3 0 obj <</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Resources<</Font<</F1<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>>>>>/Contents 4 0 R>> endobj\n" +
                    "4 0 obj <</Length 70>> stream\n" +
                    "BT\n" +
                    "/F1 18 Tf\n" +
                    "50 700 Td\n" +
                    "(DOCUMENT DE SIMULATION) Tj\n" +
                    "ET\n" +
                    "endstream\n" +
                    "endobj\n" +
                    "xref\n" +
                    "0 5\n" +
                    "0000000000 65535 f\n" +
                    "0000000009 00000 n\n" +
                    "0000000054 00000 n\n" +
                    "0000000109 00000 n\n" +
                    "0000000234 00000 n\n" +
                    "trailer <</Size 5/Root 1 0 R>>\n" +
                    "startxref\n" +
                    "353\n" +
                    "%%EOF").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            
            ByteArrayResource resource = new ByteArrayResource(miniPdf);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } else {
            // Autre format (image, texte, etc.)
            String content = "Contenu simulé pour le document : " + nomFichier + "\n"
                    + "Type : " + document.getTypeDocument() + "\n"
                    + "Ce fichier simulé a été généré car son stockage physique n'était pas disponible.";
            ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            String contentType = document.getFormat() != null ? document.getFormat() : "text/plain";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(resource.contentLength())
                    .body(resource);
        }
    }
}
