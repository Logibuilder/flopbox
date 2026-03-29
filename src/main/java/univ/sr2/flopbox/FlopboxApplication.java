package univ.sr2.flopbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import univ.sr2.flopbox.dto.TypeToken;
import univ.sr2.flopbox.dto.UserRequest;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.model.User;
import univ.sr2.flopbox.repository.ServerRepository;
import univ.sr2.flopbox.service.JwtService;
import univ.sr2.flopbox.service.RefreshTokenService;
import univ.sr2.flopbox.service.UserService;

@SpringBootApplication
public class FlopboxApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlopboxApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(ServerRepository serverRepository) {
		return args -> {
			// On vérifie d'abord que la table est vide pour ne pas les recréer à chaque redémarrage
			if (serverRepository.count() == 0) {
				// Création des serveurs (l'ID 0 indique à JPA de générer l'ID automatiquement)
				Server freeServer = new Server(0, "ftp.free.fr", "Serveur Free", 21);
				Server ubuntuServer = new Server(0, "ftp.ubuntu.com", "Serveur Ubuntu", 21);
				Server localhost = new Server(0, "localhost", "server local", 2121);

				// Sauvegarde dans la base H2
				serverRepository.save(freeServer);
				serverRepository.save(ubuntuServer);
				serverRepository.save(localhost);

				System.out.println("✅ Base de données initialisée avec les serveurs Free et Ubuntu et localhost !");
			} else {
				System.out.println("ℹ️ La base de données contient déjà des serveurs.");
			}
		};
	}
}
