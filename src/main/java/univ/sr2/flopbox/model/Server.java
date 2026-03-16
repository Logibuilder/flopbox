package univ.sr2.flopbox.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
