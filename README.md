# MedicijnMoment — Android

Eerste versie voor Android 8.0 en hoger. Nederlandstalige app met een eigen medicatielijst en dagelijkse notificaties. Geen account, netwerktoegang, advertenties of externe bibliotheken. Gegevens staan in de privéopslag van de app; cloudback-up is uitgeschakeld. Verwijderen van de app verwijdert de gegevens.

## Mogelijkheden

- Medicijn toevoegen, bewerken, verwijderen en pauzeren.
- Zelf naam, dosering, opmerking en meerdere dagelijkse tijden kiezen.
- Tijden typen in 24-uursnotatie of toevoegen met de tijdkiezer.
- Notificatie met geluid/trilling volgens de Android-meldingsinstellingen.
- Vanuit de melding afvinken als ‘Ingenomen’; ook een afvinkbaar dagoverzicht.
- Toestemmingen en een testmelding vanuit de app.
- Opnieuw inplannen na herstart (na ontgrendelen), wijziging van klok/tijdzone en app-update.

## APK bouwen met Android Studio

1. Pak dit ZIP-bestand uit. Open de map **MedicijnMoment** als project in Android Studio.
2. Gebruik JDK 17 en installeer Android SDK Platform 35 via SDK Manager.
3. Kies in de Gradle-instellingen **Local installation**, Gradle **8.11.1**. Dit project bevat geen Gradle-wrapper; installeer Gradle 8.11.1 of genereer lokaal een wrapper met `gradle wrapper --gradle-version 8.11.1` en gebruik die daarna.
4. Synchroniseer het project. De Android Gradle Plugin is 8.9.2.
5. Kies **Build > Generate App Bundles or APKs > Generate APKs** (de precieze menunaam kan per Studio-versie verschillen).
6. Het testbestand staat in `app/build/outputs/apk/debug/app-debug.apk`.
7. Kopieer het APK naar je Android-telefoon, open het en sta installatie vanuit die bron toe als Android daarom vraagt.

Met een lokaal geïnstalleerde Gradle 8.11.1 en de SDK kan ook: `gradle :app:assembleDebug :app:lintDebug`.

## APK bouwen met GitHub Actions

Maak desgewenst een privé GitHub-repository en zet de **inhoud** van deze projectmap in de hoofdmap. De workflow `.github/workflows/android.yml` bouwt een debug-APK en voert de schematests en Android lint uit. Kies **Actions > Bouw APK > Run workflow** en download na een succesvolle run het artifact **MedicijnMoment-APK**. Dit is een mogelijkheid die je zelf kunt uitvoeren; er is geen repository aangemaakt of workflow gestart.

Een debug-APK is voor eigen testen. Voor blijvend gebruik bouw je een APK met een eigen vaste signing key; bewaar die om latere updates over dezelfde installatie te kunnen installeren. Nieuwe GitHub-runs gebruiken doorgaans een andere debug-key: dan kan opnieuw installeren nodig zijn, waarbij gegevens verloren gaan.

## Eerste gebruik

1. Voeg een medicijn toe, bijvoorbeeld een fictief testmedicijn met een tijd enkele minuten in de toekomst.
2. Tik **Meldingen toestaan** en **Tijdstippen toestaan** als deze knoppen verschijnen.
3. Controleer **Stuur testmelding**. Stel geluid en trilling in via **Meldings- en geluidsinstellingen**.
4. Controleer het geplande innamemoment met het scherm uit en de app gesloten. Test ook een herstart en wacht tot de telefoon is ontgrendeld.
5. Afvinken bevestigt alleen wat je zelf hebt ingenomen; de app bepaalt geen dosering.

## Gedrag en grenzen van deze versie

Alle schema’s zijn dagelijks; weekdagen, eenmalige innames, einddatums en snoozen zijn niet inbegrepen. De klok volgt de lokale telefoon-tijdzone. Een tijd die vandaag al voorbij is, wordt voor morgen ingepland. Na herstart worden verstreken momenten niet alsnog gemeld. Tijdens de zomertijdsprong verschuift een niet-bestaande tijd vooruit; tijdens de wintertijdoverlap verschijnt één herinnering, bij de eerste occurrence.

Afvinken vóór een gepland moment onderdrukt die herinnering voor die dag. Afvinken in de melding registreert de datum van die melding, ook als die van gisteren is. Wissen van een melding geldt niet als ingenomen. Pauzeren of bewerken verwijdert zichtbare meldingen; volgende actieve momenten worden opnieuw ingepland. Wanneer je meldingen of nauwkeurige alarmen uitzet en weer aanzet, open de app om het schema opnieuw te activeren.

Android kan meldingen blokkeren via Niet storen, geluidsinstellingen of batterijbeheer. Na geforceerd stoppen moet je de app opnieuw openen. Een uitgeschakelde telefoon geeft geen meldingen. Deze eerste versie is nog niet op een echt toestel getest; verifieer de meldingen op jouw toestel voordat je erop vertrouwt.

## Validatie

De zelfstandige Java-schematests controleren invoervalidatie, dubbele tijden, middernacht, volgende dag, tijdzones en beide zomertijdovergangen. Uitvoeren:

```sh
java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/schedule-test app/src/main/java/nl/ardio/medicijnmoment/Schedule.java tests/ScheduleTest.java
java -cp /tmp/schedule-test ScheduleTest
```

Een volledige Android-build, lint en toesteltest zijn in de omgeving waarin dit project werd gemaakt niet uitgevoerd: de SDK ontbreekt en de download is geblokkeerd. Er is daarom geen APK meegeleverd. De CI-workflow voert build en lint uit zodra je hem zelf start.

Officiële technische referenties:
- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/develop/ui/views/notifications/notification-permission

## Handmatige toestelcontrole

- Twee medicijnen op dezelfde minuut: beide meldingen verschijnen.
- App naar achtergrond, scherm uit: melding verschijnt op ingesteld tijdstip.
- Naam/tijd wijzigen: alleen het nieuwe schema verschijnt.
- Pauzeren/verwijderen: volgende melding vervalt.
- Meldingen of nauwkeurige alarmen weigeren: status geeft aan dat herinneringen niet actief zijn.
- Melding afvinken: juiste dag en medicijn zijn afgevinkt; volgende dag blijft actief.
- Telefoon herstarten en ontgrendelen: volgende toekomstige melding verschijnt.
- Tijdzone wijzigen: volgende herinnering volgt de nieuwe lokale klok.
