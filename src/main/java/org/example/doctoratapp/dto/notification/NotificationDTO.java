package org.example.doctoratapp.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;
    private String message;
    private LocalDateTime dateEnvoi;
    private Boolean lu;
    private String type; // INFO, ALERTE, ACTION_REQUISE
    private String lienCible;
    private Long destinataireId;
}
