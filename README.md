# ServiceRegistry

<img style="float:right" width="100" height="100" src="docs/EF.png" alt="ServiceRegistry - ein komponent i eFormidling">

## Føremål
ServiceRegistry (SR) er [eFormidling](https://docs.digdir.no/docs/eFormidling/Introduksjon/) sitt register over kva type meldingar ein gjeven eFormidling-mottakar kan ta imot, og korleis. Dette gjer [Integrasjonspunkta](https://github.com/felleslosninger/efm-integrasjonspunkt/) i eFormidling har ei felles kjelde for protokollar og sertifikat som trengs for å utveksla meldingar med kvarandre.

## Teknologiar i bruk
- Spring Boot

## Oppstart
### Føresetnadar
- Java 25
- Maven 3
- Dedikert database: Standard er MariaDB 10.11.
- Ikkje-offentlege avhengigheitar som må vera i lokalt Maven-repository: `no.difi.virksert:virksert-common` og `no.difi.virksert:virksert-client`

### Bygging

```mvn clean install```

### Innstillingar
```
difi.move.auth.sasToken=<sas-token>
difi.move.fiks.svarut.password=<fiks-passord>
difi.move.fiks.svarut.user=<fiks-brukar>
difi.move.sign.keystore.password=<passord>
difi.move.sign.keystore.path=file:<sti-til-jks>

spring.datasource.password=<passord>
spring.datasource.url=jdbc:mariadb://<server>:<port>/<database>?serverTimezone=Europe/Oslo
spring.datasource.username=<brukar>
```

### Deployment

Pipeline håndterer bygging av docker images via mvn spring-boot:build-image. [call-buildimage.yml](https://github.com/felleslosninger/efm-service-registry/blob/main/.github/workflows/call-buildimage.yaml)

#### Testing i dev fra feature-branch
Man kan deploye rett til dev fra featurebranch for å teste ut kode. Dette kan man gjøre ved å bruke taggen [deploy-to-dev] i commitmeldingen. Det vil trigge workflowen [deploy-to-dev-only.yml](https://github.com/felleslosninger/efm-service-registry/blob/main/.github/workflows/deploy-dev-only.yml) 
Da vil image bli lastet opp i internt container registry "crutvikling", og derfra vil cd-repo deploye til dev. Dette image vil IKKE kunne bli deployet videre i test eller prod.

#### Produksjonssetting
For å produksjonssette merger man PR mot main i applikasjonsrepoet. Pipeline vil bygge image, laste det opp i container registry satt opp av plattformteamet, og publisere dette i DEV. 
Når det er ute i dev og podden starter opp uten krasj, vil det automatisk bli generert en PR mot TEST i cd-repoet. 
Når podden starter opp i TEST uten krasj, vil det automatisk bli generert en PR mot PROD i cd-repoet.  

## Grensesnitt

### REST-API
Dokumentasjon ([RestDocs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)/HTML) vert generert for "serviceregistry-server"-modulen som del av package-fasen når ein køyrer prosjekt-profilen "restdocs":

```mvn clean package -P restdocs```
