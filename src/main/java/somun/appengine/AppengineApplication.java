package somun.appengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@ServletComponentScan
public class AppengineApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppengineApplication.class, args);
	}
}
