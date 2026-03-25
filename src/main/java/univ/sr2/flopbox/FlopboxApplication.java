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

}
