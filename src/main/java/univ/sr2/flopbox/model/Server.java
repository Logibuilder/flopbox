package univ.sr2.flopbox.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Entité JPA représentant un serveur FTP stocké dans la base de données relationnelle.
 * Contient les informations de connexion de base (hôte, port, et un alias d'affichage).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;
    private String host;
    private String alias;
    private int port = 21;
}
