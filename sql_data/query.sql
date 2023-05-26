create database coffee;

use coffee;

CREATE TABLE IF NOT EXISTS `coffee`.`Kawa` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `kraj` VARCHAR(60) NOT NULL,
  `region` VARCHAR(60) NOT NULL,
  `typ` VARCHAR(60) NOT NULL,
  `aromat` float NOT NULL,
  `kwasowość` float NOT NULL,
  `słodycz` float NOT NULL,
  `ocena` float NOT NULL,
  `cenakg` FLOAT NOT NULL,
  `producent` VARCHAR(60) NOT NULL,
  `masa` INT NOT NULL,
  `stan` INT NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `coffee`.`Klient` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `nazwa_użytkownika` varchar(45) not null,
  `hasło` varchar(45) not null,
  `imię` VARCHAR(45) NOT NULL,
  `nazwisko` VARCHAR(45) NOT NULL,
  `nrtelefonu` VARCHAR(45) NOT NULL,
  `email` VARCHAR(45) NOT NULL,
  `admin` boolean not null,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `coffee`.`Koszyk` (
  `id_klienta` INT NOT NULL,
  `id_kawy` INT NOT NULL,
  `ilość` TINYINT NOT NULL,
  PRIMARY KEY (`id_klienta`, `id_kawy`),
  INDEX `a` (`id_kawy` ASC) VISIBLE,
  INDEX `b` (`id_klienta` ASC) VISIBLE,
  CONSTRAINT `a`
    FOREIGN KEY (`id_kawy`)
    REFERENCES `coffee`.`kawa` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `b`
    FOREIGN KEY (`id_klienta`)
    REFERENCES `coffee`.`Klient` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;




CREATE TABLE IF NOT EXISTS `coffee`.`Zamowienie` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `Klient_id` INT NOT NULL,
  `typ_zapłaty` ENUM("Karta", "Gotówka", "Przelew", "Blik") NOT NULL,
  `dostawa` ENUM("Kurier", "Paczkomat", "Odbiór własny") NOT NULL,
  `ulica` VARCHAR(45) NOT NULL,
  `nr_budynku` VARCHAR(10) NOT NULL,
  `nr_mieszkania` VARCHAR(10) NULL,
  `kod_pocztowy` VARCHAR(6) NOT NULL,
  `miasto` VARCHAR(45) NOT NULL,
  INDEX `fk_zamawia_klient1_idx` (`Klient_id` ASC) VISIBLE,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_zamawia_klient1`
    FOREIGN KEY (`Klient_id`)
    REFERENCES `coffee`.`Klient` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `coffee`.`Ilość` (
  `Zamowienie_id` INT NOT NULL,
  `Kawa_id` INT NOT NULL,
  `ilość` TINYINT NOT NULL,
  `opinia` TEXT,
  `ocena` INT,
  PRIMARY KEY (`Zamowienie_id`, `Kawa_id`),
  INDEX `fk_Zamowienie_has_Kawa_Kawa1_idx` (`Kawa_id` ASC) VISIBLE,
  INDEX `fk_Zamowienie_has_Kawa_Zamowienie1_idx` (`Zamowienie_id` ASC) VISIBLE,
  CONSTRAINT `fk_Zamowienie_has_Kawa_Zamowienie1`
    FOREIGN KEY (`Zamowienie_id`)
    REFERENCES `coffee`.`Zamowienie` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_Zamowienie_has_Kawa_Kawa1`
    FOREIGN KEY (`Kawa_id`)
    REFERENCES `coffee`.`Kawa` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;



	create view defaultView as
	select id as ID, kraj as Kraj, region as Region, typ as Typ, producent as Producent, stan as Stan, cenakg as Cena_Kg from kawa; 

	create view extendedView as
	select id as ID, kraj as Kraj, region as Region, typ as Typ, producent as Producent, aromat as Aromat, kwasowość as Kwasowość, słodycz as Słodycz, 
	ocena as Ocena, stan as Stan, masa as Masa_g, cenakg as Cena_Kg from kawa;

	create view commentsView as
    select Kawa_id as ID, ocena as Ocena, opinia as Opinia from ilość;

 	select * from klient;
 	select * from zamowieniilośće;
	select * from ilość;
 	select * from kawa;	
	select * from defaultView;
    select * from commentsView;
-- 	select * from extendedView;
	select klient.nazwa_użytkownika, kawa.kraj, kawa.region, kawa.typ, kawa.aromat, kawa.kwasowość, kawa.słodycz, kawa.ocena, kawa.cenakg, kawa.producent, kawa.masa, kawa.stan  from koszyk join klient on koszyk.id_klienta = klient.id join kawa on koszyk.id_kawy = kawa.id;
    select * from kawa where cenakg between 5 and 100;

