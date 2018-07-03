package cpath.service;

import cpath.config.CPathSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
@Import(BeanValidatorPluginsConfiguration.class) //JSR-303 (if controllers have bean args and validation annotations)
public class SpringFoxConfig {

  @Bean
  public Docket apiDocket() {
    return new Docket(DocumentationType.SWAGGER_2)
      .select()
//      .apis(RequestHandlerSelectors.any()) //then shows Spring actuators, swagger api,..
//      .apis(RequestHandlerSelectors.basePackage("cpath.service.web"))
      .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
      .paths(PathSelectors.any())
      .build()
      .useDefaultResponseMessages(false)
      .apiInfo(getApiInfo());
  }

  private ApiInfo getApiInfo() {
    return new ApiInfo(
      CPathSettings.getInstance().getName() + " " + CPathSettings.getInstance().getVersion(),
      "RESTful web services",
      CPathSettings.getInstance().getVersion(),
      null,
      new Contact("Pathway Commons",
        "http://www.pathwaycommons.org",
        "pathway-commons-help@googlegroups.com"
      ),
      "MIT",
      "https://raw.githubusercontent.com/PathwayCommons/cpath2/master/LICENSE",
      Collections.emptyList()
    );
  }
}