package univ.sr2.flopbox;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import univ.sr2.flopbox.model.Server;
import univ.sr2.flopbox.repository.ServerRepository;

@SpringBootApplication
public class FlopboxApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlopboxApplication.class, args);
	}

	// Cette méthode s'exécute automatiquement au lancement de l'application
	@Bean
	CommandLineRunner initDatabase(ServerRepository serverRepository) {
		return args -> {
			// On vérifie d'abord que la table est vide pour ne pas les recréer à chaque redémarrage
			if (serverRepository.count() == 0) {
				// Création des serveurs (l'ID 0 indique à JPA de générer l'ID automatiquement)
				Server freeServer = new Server(0, "ftp.free.fr", "Serveur Free", 21);
				Server ubuntuServer = new Server(0, "ftp.ubuntu.com", "Serveur Ubuntu", 21);

				// Sauvegarde dans la base H2
				serverRepository.save(freeServer);
				serverRepository.save(ubuntuServer);

				System.out.println("✅ Base de données initialisée avec les serveurs Free et Ubuntu !");
			} else {
				System.out.println("ℹ️ La base de données contient déjà des serveurs.");
			}
		};
	}
}
