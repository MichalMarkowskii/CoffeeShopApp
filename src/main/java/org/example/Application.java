package org.example;

import java.sql.*;
import java.util.*;

// Klasa przechowująca wszystkie metody związane z działaniem aplikacji
public class Application {
    // utworzenie obiektu connection, dzięki niemu połączymy się z MySQL
    private static final Connection connection = ConnectionBuilder.buildConnection();
    // aktualnie zalogowane konto użytkownika
    private static User loggedInUser;
    // lista przechowująca kawy, wrzucone do koszyka przez użytkownika
    private static final List<Coffee> basket = new ArrayList<Coffee>();

    /**
     * Metoda witająca użytkownika w sklepie. Pozwala wybrać opcję logowania, rejestracji albo
     * zamknięcia aplikację. Dokonujemy wyboru wprowadzając cyfrę, przypisaną do czynności.
     * Program pobiera input, dopóki użytkownik nie wprowadzi którejś z 3 liczb.
     */
    public static void appStart() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("+------------------------------+");
        System.out.println("| Sklep internetowy \"Kawa PWR\" |");
        System.out.println("+------------------------------+");
        System.out.println("1 - zaloguj się\n2 - zarejestruj się\n3 - zamknij aplikację");
        String input = waitForInput(scanner, new String[]{"1", "2", "3"}, "");
        switch (input) { // Po wybraniu odpowiedniej liczby, zostaje wykonana przypisana jej metoda
            case "1" -> logIn(scanner);
            case "2" -> register(scanner);
            case "3" -> System.exit(0);
        }
    }


    /**
     * Metoda rejestracji, pozwala utworzyć konto użytkownika albo administratora. Jeżeli wybrany zostanie
     * administrator, to użytkownik zostanie poproszony o podanie kodu zabezpieczeń zawartego w klasie
     * AdminAccess. W obu przypadkach, rezultatem jest zebranie scannerem od użytkownika danych o tworzonym
     * koncie. Jedyną różnicą jest, że konto administratora posiada parametr admin ustawiony na true,
     * natomiast zwykły użytkownik posiada false.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void register(Scanner scanner) {
        System.out.println("""
                Zarejestruj się jako:
                1 - użytkownik
                2 - administrator""");
        String input = waitForInput(scanner, new String[]{"1", "2"}, "");
        boolean admin = false;
        if (input.equals("2")) {
            System.out.println("Wprowadź klucz dostępu administratora:");
            // Dopóki nie zostanie podany poprawny klucz, pętla while się nie kończy. Można ją również
            // przerwać wpisując liczbę 0, cofając się do menu startowego.
            AdminAccess adminAccess = new AdminAccess();
            waitForInput(scanner, new String[]{adminAccess.getKey()}, "Klucz dostępu administratora jest nie poprawny." +
                    "\nWprowadź poprawny klucz, albo wpisz 0, żeby wrócić do okna logowania:");
            admin = true;
        }
        System.out.println("Wprowadź nazwę użytkownika:");
        String userName = scanner.nextLine();
        while (checkUsername(userName)) {
            System.out.println("Ta nazwa jest już zajęta. Wprowadź inną nazwę użytkownika:");
            userName = scanner.nextLine();
        }
        System.out.println("Wprowadź hasło:");
        String password = scanner.nextLine();
        System.out.println("Podaj swoje imię:");
        String name = scanner.nextLine();
        System.out.println("Podaj swoje nazwisko:");
        String lastName = scanner.nextLine();
        System.out.println("Podaj swój numer telefonu:");
        String phoneNumber = scanner.nextLine();
        System.out.println("Podaj swój adres e-mail:");
        String email = scanner.nextLine();
        try {
            // Po zebraniu danych, następuje połączenie z MySQL, a następnie wprowadzone zostają dane
            // użytkownika do tabeli klient
            PreparedStatement statement = connection.prepareStatement("INSERT INTO klient(nazwa_użytkownika, hasło," +
                    "imię, nazwisko, nrtelefonu, email, admin) VALUES (?,?,?,?,?,?,?);");
            statement.setString(1, userName);
            statement.setString(2, password);
            statement.setString(3, name);
            statement.setString(4, lastName);
            statement.setString(5, phoneNumber);
            statement.setString(6, email);
            statement.setBoolean(7, admin);
            statement.executeUpdate();
            System.out.println("Rejestracja zakończona pomyślnie!");
            appStart(); // Po udanej rejestracji, wracamy do menu startowego
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Metoda odpowiedzialna za zalogowanie się użytkownika. Po podaniu nazwy, sprawdzane jest czy
     * przypadkiem już taki użytkownik nie istnieje. Następnie trzeba wprowadzić hasło, po którym
     * zostajemy wprowadzeni do menu głównego aplikacji.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void logIn(Scanner scanner) {
        System.out.println("Wprowadź nazwę użytkownika:");
        String userName = scanner.nextLine();
        while (!checkUsername(userName)) {
            // Jeżeli nie znamy żadnej nazwy użytkownika, możemy się wycofać do menu start liczbą 0
            System.out.println("Taki użytkownik nie istnieje. Wprowadź właściwą nazwę użytkownika lub wpisz 0, aby cofnąć:");
            userName = scanner.nextLine();
            if ("0".equals(userName)) {
                appStart();
            }
        }
        System.out.println("Wprowadź hasło:");
        String password = scanner.nextLine();
        while (!checkPassword(userName, password)) {
            // tutaj również możliwość wycofania się do menu start przy użyciu 0
            System.out.println("Wprowadzono błędne hasło. Wprowadź hasło ponownie lub wpisz 0, aby cofnąć:");
            password = scanner.nextLine();
            if ("0".equals(password)) {
                appStart();
            }
        }
        setCurrentUser(userName); // metoda ustawia wprowadzoną nazwę użytkownika jako
        // aktualnie zalogowanego użytkownika
        previousBasket(scanner); // metoda sprawdza tabele koszyk w bazie danych i dodaje do listy basket
        // wcześniej dodane produkty
        loggedInloop(scanner); // ta metoda kieruje do menu głównego
    }

    /**
     * Metoda pozwalajaca wylogować się użytkownikowi, lecz przed tą czynnością zapisywany jest koszyk
     * do bazy danych oraz wyczyszczone zostają koszyk i aktualnie zalogowany użytkownik. Wracamy do
     * menu start.
     */
    private static void logOut() {
        saveBasket(); // zapis koszyka należącego do użytkownika w bazie danych
        loggedInUser = null;
        basket.clear();
        appStart();
    }

    /**
     * Główna metoda, stanowiąca centrum aplikacji. Stąd możemy przejść do sklepu, wyświetlić koszyk,
     * sprawdzić historię zamówień, przejść do forum oraz wylogować się. Wszystkie te czynności zostają wykonane po
     * wprowadzeniu którejś z liczb od 1 do 5. Input jest cały czas zbierany, dopóki nie dostarczymy którejś
     * z tych 4 liczb.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void loggedInloop(Scanner scanner) {
        System.out.println("+------------------------------+");
        System.out.println("| Sklep internetowy \"Kawa PWR\" |");
        System.out.println("+------------------------------+");
        System.out.println("Witaj " + loggedInUser.getName() + "!");
        System.out.println("1 - przejdź do sklepu");
        System.out.println("2 - wyświetl koszyk");
        System.out.println("3 - historia zamówień");
        System.out.println("4 - przejdź do forum");
        System.out.println("5 - wyloguj się");
        String input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5"}, "");
        switch (input) {
            case "1":
                shopLoop(scanner, "defaultView", 0);
            case "2":
                showBasket(scanner);
            case "3":
                orderHistory(scanner);
            case "4":
                showForum(scanner);
            case "5":
                logOut();
        }
    }

    /**
     * Menu koszyka, jeżeli lista koszyka basket jest pusta, wyświetlony zostaje o tym komunikat.
     * Jeżeli jednak coś jest, to dokonywana jest iteracja po liście basket, która zlicza łączną cenę
     * kaw oraz wyświetla je w konsoli po kolei. Stąd mamy możliwość powrotu do menu głównego,
     * przejścia do sklepu, usunięcia przedmiotu z koszyka, wyczyszczenie koszyka oraz złożenie
     * zamówienia.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void showBasket(Scanner scanner) {
        // Przypadek gdy koszyk jest pusty
        if (basket.size() == 0) {
            System.out.println("Koszyk jest pusty!");
        } else {
            System.out.println("Produkty w twoim koszyku:");
            System.out.println("--------------------------------");
        }
        double totalPrice = 0;
        // Wyświetlane zostają kawy z koszyka w porządku 'pozycja (nr) informacje o kawie oraz cena
        for (Coffee coffee : basket) {
            totalPrice += coffee.getPriceKg() * coffee.getMass() / 1000. * coffee.getAmount();
            System.out.println("Pozycja " + (basket.indexOf(coffee) + 1) + ". " + coffee.toString() + ", Aktualna cena: " +
                    (coffee.getPriceKg() * coffee.getMass() / 1000. * coffee.getAmount()) + " USD");
        }
        System.out.println("--------------------------------");
        System.out.println("Cena razem: " + totalPrice + " USD.");
        System.out.println("--------------------------------");
        System.out.println("1 - przejdź do strony głównej");
        System.out.println("2 - przejdź do sklepu");
        System.out.println("3 - usuń przedmiot z koszyka");
        System.out.println("4 - wyczyść koszyk");
        System.out.println("5 - złóż zamówienie");
        String input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5"}, "");
        switch (input) {
            case "1":
                loggedInloop(scanner);
            case "2":
                shopLoop(scanner, "defaultView", 0);
            case "3":
                deleteItemFromBasket(scanner);
            case "4": // po wyczyszczeniu, wracamy do widoku koszyka, tym razem pustego
                basket.clear();
                System.out.println("Koszyk został wyczyszczony.");
                System.out.println("--------------------------------");
                showBasket(scanner);
            case "5":
                placeOrder(scanner);
        }
    }

    /**
     * Menu forum, w którym możemy przejrzeć opinie na temat kaw, dodać własną lub wrócić do menu głównego.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void showForum(Scanner scanner) {
        System.out.println("+---------------------------------+");
        System.out.println("|              Forum              |");
        System.out.println("| Sklepu internetowego \"Kawa PWR\" |");
        System.out.println("+---------------------------------+");
        System.out.println("Witaj " + loggedInUser.getName() + "!");
        System.out.println("1 - przeglądaj opinie");
        System.out.println("2 - dodaj opinię");
        System.out.println("3 - wróć do strony głównej");
        String input = waitForInput(scanner, new String[]{"1", "2", "3"}, "");
        switch (input) {
            case "1":
                commentLoop(scanner);
            case "2":
                addOpinion(scanner);
            case "3":
                loggedInloop(scanner);
        }
    }

    /**
     * Łącząc się z bazą danych, wyświetlona zostaje pełna strona opinii kaw licząca 50 pozycji. Po ich przejrzeniu
     * możemy wrócić do menu forum. Możemy poruszać się po sklepie przesuwając
     * strony wpisując < i > co przesuwa o jedną stronę oraz przy użyciu << (na sam początek) i >> (na sam koniec).
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void commentLoop(Scanner scanner) {
        try {
            System.out.println("Dostępne opinie i oceny:");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM commentsView");
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("Nie dodano żadnych opinii.");
            } else {
                printResultSet(resultSet);
            }
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("1 - przejdź do strony głównej forum");
            System.out.println("-------------------------------------------");
            waitForInput(scanner, new String[]{"1"}, "");
            loggedInloop(scanner);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Łącząc się z bazą danych, wyświetlona zostaje pełna strona kaw licząca 50 pozycji. Po ich przejrzeniu
     * możemy wrócić do menu głównego, przejść do koszyka, ustawić filtry wyszukiwania, dodać produkt
     * do koszyka albo przełączyć widok pomiędzy zwykłym a rozszerzonym. Możemy poruszać się po sklepie przesuwając
     * strony wpisując < i > co przesuwa o jedną stronę oraz przy użyciu << (na sam początek) i >> (na sam koniec).
     * Jeżeli jesteśmy administratorem, pojawia się możliwość wybrania opcji sprzedającego, normalny użytkownik tego nie widzi.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType typ widoku sklepu
     * @param page     aktualna strona
     */
    private static void shopLoop(Scanner scanner, String viewType, int page) {
        try {
            System.out.println("Dostępne produkty:");
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            PreparedStatement statement1 = connection.prepareStatement("SELECT max(id) FROM kawa");
            ResultSet resultSet = statement1.executeQuery();
            resultSet.next();
            int maxPage = resultSet.getInt(1) / 50; // zdefiniowanie max. ilości stron
            PreparedStatement statement;
            // wyciągnięcie widoków z bazy danych
            if ("defaultView".equals(viewType)) {
                statement = connection.prepareStatement("SELECT * FROM defaultView WHERE ? <= id AND id <= ?");
                statement.setInt(1, page * 50);
                statement.setInt(2, page * 50 + 50);
            } else {
                statement = connection.prepareStatement("SELECT * FROM extendedView WHERE ? <= id AND id <= ?");
                statement.setInt(1, page * 50);
                statement.setInt(2, page * 50 + 50);
            }
            resultSet = statement.executeQuery();
            printResultSet(resultSet);
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("1 - przejdź do strony głównej");
            System.out.println("2 - przejdź do koszyka");
            System.out.println("3 - ustaw filtry wyszukiwania");
            System.out.println("4 - dodaj produkt do koszyka");
            if ("defaultView".equals(viewType)) {
                System.out.println("5 - przełącz na widok rozszerzony");
            } else {
                System.out.println("5 - przełącz na widok domyślny");
            }
            if (loggedInUser.isAdmin()) {
                System.out.println("6 - opcje sprzedającego");
            }
            System.out.println("-------------------------------------------");
            System.out.println("Strona " + (page + 1) + " z " + (maxPage + 1));
            System.out.println("<< | < - poprzednia strona | następna strona - > | >>");
            String input;
            if (loggedInUser.isAdmin()) {
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "<", ">", "<<", ">>"}, "");
            } else {
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "<", ">", "<<", ">>"}, "");
            }
            switch (input) {
                case "1":
                    loggedInloop(scanner);
                case "2":
                    showBasket(scanner);
                case "3":
                    setFilters(scanner, viewType);
                case "4":
                    addItemToBasket(scanner);
                case "5":
                    if ("defaultView".equals(viewType)) {
                        shopLoop(scanner, "extendedView", 0);
                    } else {
                        shopLoop(scanner, "defaultView", 0);
                    }
                case "6":
                    adminLoop(scanner);
                case "<":
                    if (page <= 0) {
                        shopLoop(scanner, viewType, 0);
                    } else {
                        shopLoop(scanner, viewType, page - 1);
                    }
                case ">":
                    if (page >= maxPage) {
                        shopLoop(scanner, viewType, page);
                    } else {
                        shopLoop(scanner, viewType, page + 1);
                    }
                case "<<":
                    shopLoop(scanner, viewType, 0);
                case ">>":
                    shopLoop(scanner, viewType, maxPage);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Menu administratora, możemy tutaj uzupełnić ilość wybranej kawy, dodać nowy produkt albo wrócić
     * do menu sklepu.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void adminLoop(Scanner scanner) {
        System.out.println("1 - Uzupełnij magazyn");
        System.out.println("2 - Dodaj nowy produkt");
        System.out.println("3 - Wróć do widoku sklepu");
        String input = waitForInput(scanner, new String[]{"1", "2", "3"}, "");
        switch (input) {
            case "1":
                refillCoffee(scanner);
            case "2":
                addCoffee(scanner);
            case "3":
                shopLoop(scanner, "defaultView", 0);
        }

    }

    /**
     * Metoda która pozwala nam wrzucić kawę do koszyka. Po wprowadzeniu ID, najpierw sprawdzane jest
     * czy takie istnieje. Następnie zostajemy zapytani jaką ilość chcemy dodać do koszyka. W tym miejscu
     * sprawdzany jest stan tej kawy na magazynie, jeżeli nasza wprowadzona ilość przekroczyła tą liczbę,
     * to zostaje o tym wyświetlona informacja.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void addItemToBasket(Scanner scanner) {
        System.out.println("Wprowadź ID produktu, który ma zostać dodany do koszyka:");
        try {
            int id = scanner.nextInt();
            if (!checkProductId(id)) {
                System.out.println("Produkt o podanym ID nie istnieje!");
                shopLoop(scanner, "defaultView", 0); //powrót do menu sklepu
            }
            System.out.println("Wprowadź ilość produktu, która ma zostać dodana:");
            int amount = scanner.nextInt();
            if (!checkProductAmount(id, amount)) {
                System.out.println("Wprowdzono za dużą lub za małą wartość!");
                shopLoop(scanner, "defaultView", 0); //powrót do menu sklepu
            }
            // Zostaje pobrana kawa z bazy danych o wybranym ID
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM kawa WHERE ID = ?");
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            // Jeżeli posiadamy już taką kawę w koszyku, to zostaje do niej dodana jedynie ilość
            for (Coffee c : basket) {
                if (c.getId() == resultSet.getInt(1)) {
                    c.setAmount(c.getAmount() + amount);
                    shopLoop(scanner, "defaultView", 0);
                }
            }
            // Jeżeli tej kawy nie było w koszyku, to zostaje dodany do niego nowy obiekt wybranej przez nas kawy
            basket.add(new Coffee(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4),
                    resultSet.getDouble(5), resultSet.getDouble(6), resultSet.getDouble(7), resultSet.getDouble(8), resultSet.getDouble(9),
                    resultSet.getString(10), resultSet.getInt(11), resultSet.getInt(12), amount));
            System.out.println("Przedmiot dodano do koszyka!");
            shopLoop(scanner, "defaultView", 0); // powrót do sklepu
        } catch (InputMismatchException e) {
            System.out.println("Wprowadzono zły typ danych (wartości muszą być liczbami całkowitymi)!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda pozwala nam usunąć kawę z koszyka. Po sprawdzeniu czy coś w nim w ogóle jest, program prosi
     * nas o wprowadzenie pozycji kawy którą chcemy usunąć, a następnie ilość. Program sprawdza i informuje
     * o podaniu niewłaściwych wartości.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void deleteItemFromBasket(Scanner scanner) {
        if (basket.isEmpty()) {
            System.out.println("W koszyku nic nie ma!");
            showBasket(scanner); // wyświetlenie koszyka
        }
        System.out.println("Wprowadź numer pozycji którą chcesz usunąć:");
        int pozycja = scanner.nextInt();
        while (basket.size() < pozycja || 1 > pozycja) {
            System.out.println("Wprowadzono niewłaściwą pozycję. Wprowadź ponownie numer pozycji którą chcesz usunąć:");
            pozycja = scanner.nextInt();
        }
        scanner.nextLine();
        System.out.println("Wprowadź liczbę sztuk produktu, którą chcesz usunąć (wpisz \"wszystko\" jeżeli chcesz usunąć cały produkt):");
        String input = scanner.nextLine();
        // możliwość usunięcia całej pozycji
        if ("wszystko".equals(input)) {
            basket.remove(pozycja - 1);
            System.out.println("Usuwanie zakończone pomyślnie!");
            showBasket(scanner);
        }
        try {
            int toBeDeleted = Integer.parseInt(input); // wartość o którą chcemy zmniejszyć ilość kawy
            if (toBeDeleted < 0 || toBeDeleted > basket.get(pozycja - 1).getAmount()) {
                System.out.println("Wprowadzono niewłaściwą wartość. Spróbuj ponownie:");
                showBasket(scanner);
            }
            basket.get(pozycja - 1).setAmount(basket.get(pozycja - 1).getAmount() - toBeDeleted);
            System.out.println("Usuwanie zakończone pomyślnie!");
            showBasket(scanner);
        } catch (NumberFormatException e) {
            System.out.println("Wprowadzono niewłaściwy typ danych. Spróbuj ponownie:");
            showBasket(scanner);
        }
    }

    /**
     * Mamy do wyboru dwie opcje, albo wyszukanie konkretnego produktu po jego parametrach albo
     * przefiltrowanie widoku w wybranym porządku.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType domyślny albo rozszerzony
     */
    private static void setFilters(Scanner scanner, String viewType) {
        System.out.println("Ustawienia filtrowania:");
        System.out.println("-----------------------");
        System.out.println("1 - Wyszukaj");
        System.out.println("2 - Sortuj");
        String input = waitForInput(scanner, new String[]{"1", "2"}, "");
        if ("2".equals(input)) {
            System.out.println("Sortuj według:");
            if ("defaultView".equals(viewType)) {
                System.out.println("----------------");
                System.out.println("1 - ID");
                System.out.println("2 - Kraj");
                System.out.println("3 - Region");
                System.out.println("4 - Typ");
                System.out.println("5 - Producent");
                System.out.println("6 - Stan");
                System.out.println("7 - Cena za kg");
                System.out.println("----------------");
                System.out.println("8 - Cofnij");
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8"}, "");
                if (input.equals("8")) {
                    shopLoop(scanner, viewType, 0);
                }
                applySorting(scanner, viewType, input);
            } else {
                System.out.println("----------------");
                System.out.println("1 - ID");
                System.out.println("2 - Kraj");
                System.out.println("3 - Region");
                System.out.println("4 - Typ");
                System.out.println("5 - Producent");
                System.out.println("6 - Aromat");
                System.out.println("7 - Kwasowość");
                System.out.println("8 - Słodycz");
                System.out.println("9 - Ocena");
                System.out.println("10 - Stan");
                System.out.println("11 - Masa paczki (g)");
                System.out.println("12 - Cena za kg");
                System.out.println("----------------");
                System.out.println("13 - Cofnij");
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8",
                        "9", "10", "11", "12", "13"}, "");
                if (input.equals("13")) {
                    shopLoop(scanner, viewType, 0);
                }
                applySorting(scanner, viewType, input);
            }
        } else {
            setSearch(scanner, viewType);
        }
    }

    /**
     * Tworzone są dwie tablice, które odpowiadają parametrom kawy w konkretnym widoku. Domyślnie są
     * one ustawione jako -, czyli brak zastosowanego filtru.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType domyślny albo rozszerzony
     */
    private static void setSearch(Scanner scanner, String viewType) {
        String[] defaultStatus = new String[]{"-", "-", "-", "-", "-", "-", "-"};
        String[] extendedStatus = new String[]{"-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"};
        String input;
        if ("defaultView".equals(viewType)) {
            printSearchFilters(viewType, defaultStatus); // wyświetlone zostają aktualnie użyte filtry
            input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"}, "");
            if ((input.equals("8"))) {
                shopLoop(scanner, viewType, 0);
            }
            if ((input.equals("9"))) {
                applySearch(scanner, viewType, defaultStatus);
            }
            if ((input.equals("10"))) {
                setSearch(scanner, viewType);
            }
            while (true) {
                if (input.equals("7") || input.equals("6")) {
                    System.out.println("Wpisz dolną granicę przedziału:");
                    String lower = scanner.nextLine();
                    System.out.println("Wpisz górną granicę przedziału:");
                    defaultStatus[Integer.parseInt(input) - 1] = lower + " - " + scanner.nextLine();
                } else {
                    System.out.println("Ustaw szukaną wartość (wpisz \"-\" aby wyczyścić):");
                    // po wybraniu numeru, mamy możliwość ustawienie filtra w wybranym przez nas parametrze
                    defaultStatus[Integer.parseInt(input) - 1] = scanner.nextLine();
                }
                printSearchFilters(viewType, defaultStatus);
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"}, "");
                if ((input.equals("8"))) {
                    shopLoop(scanner, viewType, 0);
                }
                if ((input.equals("9"))) {
                    applySearch(scanner, viewType, defaultStatus);
                }
                if ((input.equals("10"))) {
                    setSearch(scanner, viewType);
                }
            }
        } else {
            // to samo, tylko że dla widoku rozszerzonego
            printSearchFilters(viewType, extendedStatus);
            input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                    "11", "12", "13", "14", "15"}, "");
            if ((input.equals("13"))) {
                shopLoop(scanner, viewType, 0);
            }
            if ((input.equals("14"))) {
                applySearch(scanner, viewType, extendedStatus);
            }
            if ((input.equals("15"))) {
                setSearch(scanner, viewType);
            }
            while (true) {
                if (input.equals("12") || input.equals("11") || input.equals("10") || input.equals("9") ||
                        input.equals("8") || input.equals("7") || input.equals("6")) {
                    System.out.println("Wpisz dolną granicę przedziału:");
                    String lower = scanner.nextLine();
                    System.out.println("Wpisz górną granicę przedziału:");
                    extendedStatus[Integer.parseInt(input) - 1] = lower + " - " + scanner.nextLine();
                } else {
                    System.out.println("Ustaw szukaną wartość (wpisz \"-\" aby wyczyścić):");
                    extendedStatus[Integer.parseInt(input) - 1] = scanner.nextLine();
                }
                printSearchFilters(viewType, extendedStatus);
                input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                        "11", "12", "13", "14", "15"}, "");
                if ((input.equals("13"))) {
                    shopLoop(scanner, viewType, 0);
                }
                if ((input.equals("14"))) {
                    applySearch(scanner, viewType, extendedStatus);
                }
                if ((input.equals("15"))) {
                    setSearch(scanner, viewType);
                }
            }
        }
    }


    /**
     * Ta metoda wyświetla wyszystkie aktualnie zastosowane przez nas filtry.
     *
     * @param viewType typ widoku zwykły bądź domyślny
     * @param status   tablica, zawierająca aktualnie ustawione filtry
     */
    private static void printSearchFilters(String viewType, String[] status) {
        System.out.println("Ustaw wartości:");
        System.out.println("--------------------------");
        if ("defaultView".equals(viewType)) {
            System.out.println("1 - ID: " + status[0]);
            System.out.println("2 - Kraj: " + status[1]);
            System.out.println("3 - Region: " + status[2]);
            System.out.println("4 - Typ: " + status[3]);
            System.out.println("5 - Producent: " + status[4]);
            System.out.println("6 - Stan: " + status[5]);
            System.out.println("7 - Cena za kg: " + status[6]);
            System.out.println("--------------------------");
            System.out.println("8 - Cofnij");
            System.out.println("9 - Wyszukaj");
            System.out.println("10 - Wyszyść filtry");
        } else {
            System.out.println("1 - ID: " + status[0]);
            System.out.println("2 - Kraj: " + status[1]);
            System.out.println("3 - Region: " + status[2]);
            System.out.println("4 - Typ: " + status[3]);
            System.out.println("5 - Producent: " + status[4]);
            System.out.println("6 - Aromat: " + status[5]);
            System.out.println("7 - Kwasowość: " + status[6]);
            System.out.println("8 - Słodycz: " + status[7]);
            System.out.println("9 - Ocena: " + status[8]);
            System.out.println("10 - W magazynie: " + status[9]);
            System.out.println("11 - Masa paczki (g): " + status[10]);
            System.out.println("12 - Cena za kg: " + status[11]);
            System.out.println("--------------------------");
            System.out.println("13 - Cofnij");
            System.out.println("14 - Wyszukaj");
            System.out.println("15 - Wyszyść filtry");
        }
    }

    /**
     * Metoda ta zatwierdza użycie wprowadzonych filtrów, oraz pobiera wyszukaną kawę z bazy danych.
     * Jeżeli taka nie istnieje, to wyświetlany jest o tym komunikat. Wyświetlany jest również
     * komunikat o braku zastosowanych filtrów. Jeżeli wszystko się zgadza, to program przechodzi do
     * metody afterFilterLoop.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType zwykły bądź rozszerzony
     * @param status   tablica aktualnie użytych filtrów
     */
    private static void applySearch(Scanner scanner, String viewType, String[] status) {
        String query = buildSearchQuery(viewType, status);
        try {
            PreparedStatement statement;
            if (!"".equals(query)) {
                if ("defaultView".equals(viewType)) {
                    statement = connection.prepareStatement("SELECT * FROM defaultView WHERE " + query);
                } else {
                    statement = connection.prepareStatement("SELECT * FROM extendedView WHERE " + query);
                }
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.isBeforeFirst()) {
                    System.out.println("Nie znaleziono produktu spełniającego żądane kryteria.");
                    System.out.println("------------------------------------------------------");
                } else {
                    System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
                    printResultSet(resultSet);
                    System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
                }
            } else {
                System.out.println("Nie wprowadzono żadnych filtrów!");
            }
            afterFilterLoop(scanner, viewType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Z tablicy status sprawdzane jest jakie filtry chcemy zastosować. Do ustalenia, na które
     * wyrażenia stosujemy filtrowanie, pomaga lista columns, do której dodajemy numery odpowiadające
     * numerom filtrowanych parametrów. Parametry ze znakiem "-", nie są wliczane. Następnie do
     * zmiennej query dodawane są stwierdzenia w języku MySQL, odpowiadające numerom parametrów
     * przechowywanych w liście columns.
     *
     * @param viewType domyślny albo rozszerzony
     * @param status   tablica z aktualnie użytymi filtrami
     * @return zwraca ciąg znaków String, który pozwala zastosować użyte filtry w MySQL
     */
    private static String buildSearchQuery(String viewType, String[] status) {
        List<Integer> columns = new ArrayList<Integer>();
        String query = "";
        // sprawdzenie czy tablica status posiada jakieś inne elementy niż "-"
        for (int i = 0; i <= status.length - 1; i++) {
            if (!"-".equals(status[i])) {
                columns.add(i + 1);
            }
        }
        // Warunki, które budują sformułowanie w języku MySQL dla widoku zwykłego
        if ("defaultView".equals(viewType)) {
            if (columns.contains(1)) {
                query += "ID = \"" + status[0] + "\" ";
            }
            if (columns.contains(2)) {
                query += "AND Kraj LIKE \"%" + status[1] + "%\" ";
            }
            if (columns.contains(3)) {
                query += "AND Region LIKE \"%" + status[2] + "%\" ";
            }
            if (columns.contains(4)) {
                query += "AND Typ LIKE \"%" + status[3] + "%\" ";
            }
            if (columns.contains(5)) {
                query += "AND Producent LIKE \"%" + status[4] + "%\" ";
            }
            if (columns.contains(6)) {
                String[] values = status[5].split(" - ");
                query += "AND (Stan BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(7)) {
                String[] values = status[6].split(" - ");
                query += "AND (Cena_kg BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (!"".equals(query)) {
                if (query.charAt(0) == 'A') {
                    query = query.substring(4);
                }
            }
            // Warunki, które budują sformułowanie w języku MySQL dla widoku rozszerzonego
        } else {
            if (columns.contains(1)) {
                query += "ID = \"" + status[0] + "\" ";
            }
            if (columns.contains(2)) {
                query += "AND Kraj LIKE \"%" + status[1] + "%\" ";
            }
            if (columns.contains(3)) {
                query += "AND Region LIKE \"%" + status[2] + "%\" ";
            }
            if (columns.contains(4)) {
                query += "AND Typ LIKE \"%" + status[3] + "%\" ";
            }
            if (columns.contains(5)) {
                query += "AND Producent LIKE \"%" + status[4] + "%\" ";
            }
            if (columns.contains(6)) {
                String[] values = status[5].split(" - ");
                query += "AND (aromat BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(7)) {
                String[] values = status[6].split(" - ");
                query += "AND (kwasowość BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(8)) {
                String[] values = status[7].split(" - ");
                query += "AND (słodycz BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(9)) {
                String[] values = status[8].split(" - ");
                query += "AND (Ocena BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(10)) {
                String[] values = status[9].split(" - ");
                query += "AND (Stan BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(11)) {
                String[] values = status[10].split(" - ");
                query += "AND (Masa_g BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (columns.contains(12)) {
                String[] values = status[11].split(" - ");
                query += "AND (Cena_kg BETWEEN " + values[0] + " AND " + values[1] + ")";
            }
            if (!"".equals(query)) {
                if (query.charAt(0) == 'A') {
                    query = query.substring(4);
                }
            }
        }
        return query;
    }

    /**
     * Metoda pozwalajaca nam na wybór sortowania pomiędzy rosnącym, a malejącym.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType zwykły albo rozszerzony
     * @param filter   rodzaj filtrowania
     */
    private static void applySorting(Scanner scanner, String viewType, String filter) {
        System.out.println("Rosnąco (alfabetycznie), czy malejąco (odwrotnie alfabetycznie)?");
        System.out.println("----------------------------------------");
        System.out.println("1 - Rosnąco (alfabetycznie)");
        System.out.println("2 - Malejąco (odwrotnie alfabetycznie)");
        System.out.println("----------------------------------------");
        System.out.println("3 - Cofnij");
        String input = waitForInput(scanner, new String[]{"1", "2", "3"}, "");
        if ("3".equals(input)) {
            setFilters(scanner, viewType);
        }
        PreparedStatement statement;
        try {
            if ("defaultView".equals(viewType)) {
                if ("1".equals(input)) {
                    statement = connection.prepareStatement("SELECT * FROM defaultView ORDER BY ?");
                } else {
                    statement = connection.prepareStatement("SELECT * FROM defaultView ORDER BY ? DESC ");
                }
            } else {
                if ("1".equals(input)) {
                    statement = connection.prepareStatement("SELECT * FROM extendedView ORDER BY ?");
                } else {
                    statement = connection.prepareStatement("SELECT * FROM extendedView ORDER BY ? DESC ");
                }
            }
            statement.setInt(1, Integer.parseInt(filter));
            ResultSet resultSet = statement.executeQuery();
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            printResultSet(resultSet);
            System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
            afterFilterLoop(scanner, viewType);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda pozwala nam przejść do strony głównej, do koszyka, ustawić filtry wyszukiwania, dodać
     * produkt do koszyka oraz przełączyć widok.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param viewType zwykły lub rozszerzony
     */
    private static void afterFilterLoop(Scanner scanner, String viewType) {
        System.out.println("1 - przejdź do strony głównej");
        System.out.println("2 - przejdź do koszyka");
        System.out.println("3 - ustaw filtry wyszukiwania");
        System.out.println("4 - dodaj produkt do koszyka");
        if ("defaultView".equals(viewType)) {
            System.out.println("5 - przełącz na widok rozszerzony");
        } else {
            System.out.println("5 - przełącz na widok domyślny");
        }
        String input = waitForInput(scanner, new String[]{"1", "2", "3", "4", "5"}, "");
        switch (input) {
            case "1":
                loggedInloop(scanner);
            case "2":
                showBasket(scanner);
            case "3":
                setFilters(scanner, viewType);
            case "4":
                addItemToBasket(scanner);
            case "5":
                if ("defaultView".equals(viewType)) {
                    shopLoop(scanner, "extendedView", 0);
                } else {
                    shopLoop(scanner, "defaultView", 0);
                }
        }
    }

    /**
     * Metoda umożliwia złożenie zamówienia na produkty z koszyka. Jeżeli nic w nim nie ma, wyskakuje
     * odpowiedni komunikat i wracamy do menu koszyka. Jeżeli coś jest to wyświetlona zostaje cena
     * produktów, możemy ją zaakeceptować albo cofnąć. Po zaakceptowaniu wprowadzamy swoje dane
     * wysyłkowe, sposób dostawy oraz zapłaty. Na końcu wszystkie te dane zostają wyświetlony, a my
     * możemy albo je zaakceptować, albo cofnąć.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void placeOrder(Scanner scanner) {
        double totalPrice = 0;
        if (basket.isEmpty()) {
            System.out.println("W koszyku nic nie ma!");
            showBasket(scanner);
        }
        for (Coffee coffee : basket) {
            totalPrice += coffee.getPriceKg() * coffee.getMass() / 1000. * coffee.getAmount();
        }
        System.out.println("Razem do zapłaty: " + totalPrice + " USD.");
        System.out.println("-------------------------------------");
        System.out.println("1 - zatwierdź");
        System.out.println("2 - cofnij");
        String input = waitForInput(scanner, new String[]{"1", "2"}, "");
        if ("2".equals(input)) {
            showBasket(scanner);
        }
        System.out.println("Uzupełnij dane do przesyłki.");
        System.out.println("Wprowadż ulicę:");
        String street = scanner.nextLine();
        System.out.println("Wprowadź numer budynku:");
        String building = scanner.nextLine();
        System.out.println("Wprowadź numer mieszkania (wpisz \"brak\" jeżeli nie dotyczy):");
        String apartment = scanner.nextLine();
        System.out.println("Wprowadź kod pocztowy:");
        String postalCode = scanner.nextLine();
        System.out.println("Wprowadź miasto:");
        String city = scanner.nextLine();
        System.out.println("Wprowadzone dane:\n-----------------------------------\nUlica: " + street + "\nBudynek: " + building + "\nMieszkanie: " +
                apartment + "\nKod pocztowy: " + postalCode + "\nMiasto: " + city + "\n-----------------------------------");
        System.out.println("Czy zatwierdzasz poprawność danych?");
        System.out.println("-----------------------------------");
        System.out.println("1 - zatwierdź");
        System.out.println("2 - cofnij");
        input = waitForInput(scanner, new String[]{"1", "2"}, "");
        if ("2".equals(input)) {
            placeOrder(scanner);
        }
        String[] deliveryMethods = new String[]{"Kurier", "Paczkomat", "Odbiór własny"};
        System.out.println("Wybierz sposób dostawy:");
        System.out.println("-----------------------");
        System.out.println("1 - Kurier");
        System.out.println("2 - Paczkomat");
        System.out.println("3 - Odbiór własny");
        String delivery = waitForInput(scanner, new String[]{"1", "2", "3"}, "");
        String[] paymentMethods = new String[]{"Karta", "Gotówka", "Przelew", "Blik"};
        System.out.println("Wybierz sposób zapłaty:");
        System.out.println("-----------------------");
        System.out.println("1 - Karta");
        System.out.println("2 - Gotówka");
        System.out.println("3 - Przelew");
        System.out.println("4 - Blik");
        String payment = waitForInput(scanner, new String[]{"1", "2", "3", "4"}, "");
        System.out.println("Podsumowanie zamówienia:");
        System.out.println("-----------------------------------");
        System.out.println("Wprowadzone dane:\nUlica: " + street + "\nBudynek: " + building + "\nMieszkanie: " +
                apartment + "\nKod pocztowy: " + postalCode + "\nMiasto: " + city);
        System.out.println("Sposób dostawy: " + deliveryMethods[Integer.parseInt(delivery) - 1]);
        System.out.println("Sposób zapłaty: " + paymentMethods[Integer.parseInt(payment) - 1]);
        System.out.println("-----------------------------------");
        System.out.println("Czy zatwierdzasz poprawność danych?");
        System.out.println("-----------------------------------");
        System.out.println("1 - zatwierdź");
        System.out.println("2 - cofnij");
        input = waitForInput(scanner, new String[]{"1", "2"}, "");
        if ("2".equals(input)) {
            placeOrder(scanner);
        }
        registerOrder(scanner, payment, delivery, street, building, apartment, postalCode, city, deliveryMethods, paymentMethods);
    }

    /**
     * Metoda przyjmuje parametry zamówienia, które są zapisywane w MySQL w tabeli zamówienia.
     *
     * @param scanner         skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param payment         String rodzaj płatności
     * @param delivery        String rodzaj dostawy
     * @param street          String ulica na której mieszka zamawiający
     * @param building        String numer budynku zamawiającego
     * @param apartment       String numer mieszkania zamawiającego
     * @param postalCode      String kod pocztowy zamawiającego
     * @param city            String miasto zamawiającego
     * @param deliveryMethods String wszystkie możliwe opcje dostawy
     * @param paymentMethods  String wszystkie możliwe opcje płatności
     */
    private static void registerOrder(Scanner scanner, String payment, String delivery, String street, String building, String apartment,
                                      String postalCode, String city, String[] deliveryMethods, String[] paymentMethods) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO zamowienie(Klient_id, typ_zapłaty, " +
                    "dostawa, ulica, nr_budynku, nr_mieszkania, kod_pocztowy, miasto) VALUES (?,?,?,?,?,?,?,?);");
            statement.setInt(1, loggedInUser.getId());
            statement.setString(2, paymentMethods[Integer.parseInt(payment) - 1]);
            statement.setString(3, deliveryMethods[Integer.parseInt(delivery) - 1]);
            statement.setString(4, street);
            statement.setString(5, building);
            if ("brak".equals(apartment)) {
                statement.setString(6, null);
            } else {
                statement.setString(6, apartment);
            }
            statement.setString(7, postalCode);
            statement.setString(8, city);
            statement.executeUpdate();
            Statement statement2 = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement2.executeQuery("SELECT * FROM zamowienie");
            resultSet.afterLast();
            resultSet.previous();
            int orderId = resultSet.getInt(1);
            statement = connection.prepareStatement("INSERT INTO ilość(Zamowienie_id, Kawa_id, ilość) VALUES (?,?,?)");
            PreparedStatement statement3 = connection.prepareStatement("UPDATE kawa SET stan = ? WHERE id = ?");
            for (Coffee coffee : basket) {
                statement.setInt(1, orderId);
                statement.setInt(2, coffee.getId());
                statement.setInt(3, coffee.getAmount());
                statement.executeUpdate();
                statement3.setInt(1, coffee.getInStorage() - coffee.getAmount());
                statement3.setInt(2, coffee.getId());
                statement3.executeUpdate();
            }
            basket.clear();
            System.out.println("Zamówienie złożone pomyślnie!");
            loggedInloop(scanner);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda daje nam wgląd w historię zamówień aktualnie zalogowanego użytkownika. Jeżeli ten nie
     * złożył jeszcze zamówienia, to wyświetalny jest komunikat o ich braku. Jeżeli takie są, to
     * są one wyświetlane po kolei w pozycjach. Możemy wybrać numer pozycji, żeby zobaczyć szczegóły tego
     * zamówienia. Literą b, cofamy się.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void orderHistory(Scanner scanner) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM zamowienie WHERE Klient_id = ?");
            statement.setInt(1, loggedInUser.getId());
            ResultSet resultSet = statement.executeQuery();
            int position = 1;
            if (!resultSet.isBeforeFirst()) {
                System.out.println("Nie złożono jeszcze żadnego zamówienia.");
                System.out.println("---------------------------------------");
                System.out.println("b - powrót");
                String input = waitForInput(scanner, new String[]{"b"}, "");
                loggedInloop(scanner);
            } else {
                System.out.println("Poprzednie zamówienia:");
                System.out.println("-----------------------------------------------------------");
                while (resultSet.next()) {
                    System.out.println("Pozycja " + position + ". Numer zamówienia: " + resultSet.getInt(1) + ", Sposób zapłaty: " + resultSet.getString(3)
                            + ", Sposób dostawy: " + resultSet.getString(4));
                    position++;
                }
                String[] options = new String[position];
                position -= 2;
                options[position + 1] = "b";
                while (position >= 0) {
                    options[position] = String.valueOf(position + 1);
                    position--;
                }
                System.out.println("-----------------------------------------------------------");
                System.out.println("Aby wyświetlić szczegóły zamówienia wprowadź numer pozycji.");
                System.out.println("-----------------------------------------------------------");
                System.out.println("b - powrót");
                String input = waitForInput(scanner, options, "");
                if ("b".equals(input)) {
                    loggedInloop(scanner);
                }
                showDetails(scanner, Integer.parseInt(input));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Metoda pobiera z bazy danych MySQL informacje na temat pozycji zamówienia, użytej jako parametr
     * metody.
     *
     * @param scanner  skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param position numer pozycji zamówienia
     */
    private static void showDetails(Scanner scanner, int position) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM zamowienie WHERE Klient_id = ?");
            statement.setInt(1, loggedInUser.getId());
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Widok szczegółowy.");
            System.out.println("------------------------------");
            while (position > 0) {
                resultSet.next();
                position--;
            }
            System.out.println("Numer zamówienia: " + resultSet.getInt(1) + "\nSposób zapłaty: " + resultSet.getString(3)
                    + "\nSposób dostawy: " + resultSet.getString(4) + "\nUlica: " + resultSet.getString(5)
                    + "\nNumer budynku: " + resultSet.getString(6) + "\nNumer mieszkania: " + resultSet.getString(7)
                    + "\nKod pocztowy: " + resultSet.getString(8) + "\nMiasto: " + resultSet.getString(9));
            System.out.println("------------------------------");
            System.out.println("Produkty:");
            statement = connection.prepareStatement("SELECT kawa.id, kawa.kraj, kawa.region, kawa.producent, ilość.ilość, kawa.cenakg * kawa.masa / 1000 * ilość.ilość " +
                    "FROM kawa JOIN ilość ON kawa.id = ilość.Kawa_id WHERE ilość.Zamowienie_id = ?");
            statement.setInt(1, resultSet.getInt(1));
            resultSet = statement.executeQuery();
            double totalPrice = 0;
            while (resultSet.next()) {
                System.out.println("ID: " + resultSet.getInt(1) + ", Kraj: " + resultSet.getString(2) + ", Region: " + resultSet.getString(3) +
                        ", Producent: " + resultSet.getString(4) + ", Ilość zamówiona: " + resultSet.getInt(5) + ", Cena: " + resultSet.getDouble(6)
                        + " USD");
                totalPrice += resultSet.getDouble(6);
            }
            System.out.println("-------------------------------------------");
            System.out.println("Razem zapłacono: " + totalPrice + " USD.");
            System.out.println("-------------------------------------------");
            System.out.println("1 - cofnij");
            waitForInput(scanner, new String[]{"1"}, "");
            orderHistory(scanner);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda łączy się z bazą danych MySQL i sprawdza czy nazwa użytkownika, użyta jako parametr metody,
     * nie istnieje już w tabeli klient. Jeżeli istnieje zwraca true, jeżeli nie to false.
     *
     * @param userName String
     * @return boolean jeśli program znajdzie szukany parmaetr, zwraca true. W przeciwnym wypadku zwraca false.
     */
    private static boolean checkUsername(String userName) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT nazwa_użytkownika FROM klient WHERE nazwa_użytkownika = ?");
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return false;
            }
            if (Objects.equals(resultSet.getString(1), userName)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sprawdza czy hasło zgadza się z nazwą użytkownika. Do tego łączy się z bazą danych z tabelą klient
     * żeby sprawdzić poprawność wprowadzonych parametrów.
     *
     * @param userName String ciąg znaków reprezentujący nazwę użytkownika
     * @param password String ciąg znaków reprezentujący hasło użytkownika
     * @return boolean jeśli program znajdzie szukany parmaetr, zwraca true. W przeciwnym wypadku zwraca false.
     */
    private static boolean checkPassword(String userName, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT hasło FROM klient WHERE nazwa_użytkownika = ?");
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            if (Objects.equals(resultSet.getString(1), password)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sprawdza, czy wprowadzony parmaetr id porduktu istnieje w bazie danych
     *
     * @param id int id kawy
     * @return boolean jeśli program znajdzie szukany parmaetr, zwraca true. W przeciwnym wypadku zwraca false.
     */
    private static boolean checkProductId(int id) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM kawa WHERE id = ?");
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return false;
            }
            if (Objects.equals(resultSet.getInt(1), id)) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sprawdza czy wprowadzona ilość zgadza się z faktycznym stanem dla id kawy.
     *
     * @param id     int id kawy
     * @param amount int ilość kawy
     * @return boolean jeśli program znajdzie szukany parmaetr, zwraca true. W przeciwnym wypadku zwraca false.
     */
    private static boolean checkProductAmount(int id, int amount) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT stan FROM kawa WHERE id = ?");
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) >= amount & amount > 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Ustawia nowo zalogowanego użytkownika jako aktualnego użytkownika. Do tego pobiera jego dane z
     * tabeli klient i wprowadza je przy tworzeniu obiektu User.
     *
     * @param userName String nazwa użytkownika
     */
    private static void setCurrentUser(String userName) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM klient WHERE nazwa_użytkownika = ?");
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            User user = new User(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3),
                    resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7), resultSet.getBoolean(8));
            loggedInUser = user;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Metoda pozwalająca wyświetlić wynik użytego sformułowania w MySQL
     *
     * @param resultSet zbiór danych wyjściowych, otrzymanych z wykonanengo polecenia w MySQL
     * @throws SQLException
     */
    private static void printResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1)
                    System.out.print(";\t\t\t");
                String columnValue = resultSet.getString(i);
                System.out.print(rsmd.getColumnName(i) + ": " + columnValue);
            }
            System.out.println("");
        }
        System.out.println("");

    }

    /**
     * Metoda pozwala na uzupełnienie ilości kawy w sklepie. Zbierane jest id od użytkownika, jeżeli jest
     * one nie poprawne, to zwracany jest komunikat i program wraca do menu sklepu. Następnie proszeni
     * jesteśmy o wprowadzenie ilości o jaką chcemy zmienić stan naszego produktu. Sprawdzane jest również
     * czy przypadkiem stan kawy po wprowadzeniu jest ujemny.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void refillCoffee(Scanner scanner) {
        try {
            System.out.println("Wprowadź ID produktu, którego stan ma zostać zmieniony:");
            int id = scanner.nextInt();
            if (!checkProductId(id)) {
                System.out.println("Produkt o podanym ID nie istnieje!");
                shopLoop(scanner, "defaultView", 0);
            }
            System.out.println("Wprowadź zmianę stanu produktu:");
            int amount = scanner.nextInt();
            PreparedStatement statement2 = connection.prepareStatement("SELECT stan FROM kawa WHERE id = ?");
            statement2.setInt(1, id);
            ResultSet resultSet = statement2.executeQuery();
            resultSet.next();
            if (resultSet.getInt(1) + amount < 0) {
                System.out.println("Stan produktu nie może byc ujemny!");
                shopLoop(scanner, "defaultView", 0);
            }
            PreparedStatement statement = connection.prepareStatement("UPDATE kawa SET stan = ? WHERE id = ?");
            statement.setInt(1, resultSet.getInt(1) + amount);
            statement.setInt(2, id);
            statement.executeUpdate();
            System.out.println("Pomyślnie zmieniono stan produktu!");
            shopLoop(scanner, "defaultView", 0);
        } catch (InputMismatchException e) {
            scanner.nextLine();
            System.out.println("Wprowadzono zły typ danych (wartości muszą być liczbami całkowitymi)!");
            refillCoffee(scanner);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda zapisuje kawy z listy basket do tabeli w MySQL, przypisujac im nazwę aktualnei zalogowanego
     * użytkownika. Wtedy po następny zalogowaniu, będziemy mogli je z powrotem umieścić w koszyku.
     */
    private static void saveBasket() {
        for (Coffee coffee : basket) {
            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO koszyk(id_klienta, id_kawy, " +
                        "ilość) VALUES (?,?,?)");
                statement.setInt(1, loggedInUser.getId());
                statement.setInt(2, coffee.getId());
                statement.setInt(3, coffee.getAmount());
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Metoda pobiera z tabeli koszyk w MySQL kawy odpowiadające aktualnie zalogowanemu użytkownikowi.
     * Jeżeli ten użytkownik nic nie zamawiał, to program przechodzi od razu do menu głównego. Następnie
     * przechodząc po każdym wierszu, dodawana jest kawa do koszyka odpowiadajaca zalogowanemu użytkownikowi.
     * Po zapisaniu, produkty dla tego użytkownika zostają usunięte z tabeli koszyk.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void previousBasket(Scanner scanner) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT kawa.id, kawa.kraj, " +
                    "kawa.region, kawa.typ, kawa.aromat, kawa.kwasowość, kawa.słodycz, kawa.ocena, kawa.cenakg, " +
                    "kawa.producent, kawa.masa, kawa.stan  FROM koszyk JOIN klient ON koszyk.id_klienta = klient.id JOIN " +
                    "kawa ON koszyk.id_kawy = kawa.id WHERE klient.nazwa_użytkownika = ?");
            statement.setString(1, loggedInUser.getUserName());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next() == false) {
                loggedInloop(scanner);
            }
            do {
                PreparedStatement statement1 = connection.prepareStatement("SELECT ilość FROM koszyk WHERE id_kawy = ?");
                statement1.setInt(1, resultSet.getInt(1));
                ResultSet resultSet1 = statement1.executeQuery();
                resultSet1.next();
                basket.add(new Coffee(resultSet.getInt(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4),
                        resultSet.getDouble(5), resultSet.getDouble(6), resultSet.getDouble(7), resultSet.getDouble(8), resultSet.getDouble(9),
                        resultSet.getString(10), resultSet.getInt(11), resultSet.getInt(12), resultSet1.getInt(1)));
            } while (resultSet.next());
            PreparedStatement statement2 = connection.prepareStatement("DELETE FROM koszyk WHERE id_klienta = ?");
            statement2.setInt(1, loggedInUser.getId());
            statement2.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoda zbiera informacje o kawie, a następnie dodaję ją do bazy danych o indeksie o jeden większym
     * od najwyższego id. Program sprawdza czy kawa przyapdkiem nie znajduje się już w bazie.
     *
     * @param scan skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void addCoffee(Scanner scan) {
        try {
            Coffee newCoffee = new Coffee();
            System.out.println("Podaj kraj pochodzenia kawy:");
            newCoffee.setCountry(scan.nextLine());
            System.out.println("Podaj region z którego pochodzi kawa:");
            newCoffee.setRegion(scan.nextLine());
            System.out.println("Podaj typ kawy:");
            newCoffee.setType(scan.nextLine());
            System.out.println("Wprowadź wartość aromatu:");
            newCoffee.setAroma(scan.nextDouble());
            System.out.println("Wprowadź wartość kwasowatości:");
            newCoffee.setAcidity(scan.nextDouble());
            System.out.println("Wprowadź wartość słodkości:");
            newCoffee.setSweetness(scan.nextDouble());
            System.out.println("Wprowadź ocenę kawy:");
            newCoffee.setScore(scan.nextDouble());
            System.out.println("Wprowadź cenę za kg:");
            newCoffee.setPriceKg(scan.nextDouble());
            scan.nextLine();
            System.out.println("Podaj producenta:");
            newCoffee.setProducer(scan.nextLine());
            System.out.println("Podaj masę kawy:");
            newCoffee.setMass(scan.nextInt());
            System.out.println("Podaj jaką ilość paczek chcesz wprowadzić:");
            newCoffee.setInStorage(scan.nextInt());
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM kawa WHERE kraj = ? AND" +
                    " region = ? AND typ = ? AND aromat = ? AND kwasowość = ? AND słodycz = ? AND ocena = ? AND" +
                    " cenakg = ? AND producent = ? AND masa = ?");
            statement.setString(1, newCoffee.getCountry());
            statement.setString(2, newCoffee.getRegion());
            statement.setString(3, newCoffee.getType());
            statement.setDouble(4, newCoffee.getAroma());
            statement.setDouble(5, newCoffee.getAcidity());
            statement.setDouble(6, newCoffee.getSweetness());
            statement.setDouble(7, newCoffee.getScore());
            statement.setDouble(8, newCoffee.getPriceKg());
            statement.setString(9, newCoffee.getProducer());
            statement.setInt(10, newCoffee.getMass());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst()) {
                System.out.println("Produkt nie został dodany, ponieważ już istnieje.");
                shopLoop(scan, "defaultView", 0);
            } else {
                statement = connection.prepareStatement("INSERT INTO kawa(kraj, " +
                        "region, typ, aromat, kwasowość, słodycz, ocena, cenakg, producent, masa, stan) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                statement.setString(1, newCoffee.getCountry());
                statement.setString(2, newCoffee.getRegion());
                statement.setString(3, newCoffee.getType());
                statement.setDouble(4, newCoffee.getAroma());
                statement.setDouble(5, newCoffee.getAcidity());
                statement.setDouble(6, newCoffee.getSweetness());
                statement.setDouble(7, newCoffee.getScore());
                statement.setDouble(8, newCoffee.getPriceKg());
                statement.setString(9, newCoffee.getProducer());
                statement.setInt(10, newCoffee.getMass());
                statement.setInt(11, newCoffee.getInStorage());
                statement.executeUpdate();
                System.out.println("Pomyślnie dodano nowy produkt do sklepu!");
                shopLoop(scan, "defaultType", 0);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InputMismatchException e2) {
            System.out.println("Wprowadzono niewłaściwy typ danych spróbuj ponownie.");
            shopLoop(scan, "defaultView", 0);
        }
    }

    /**
     * Metoda zbiera informacje o zamówieniu i kawie, a następnie dodaję ododaje ocenę i opinię kupującego. Program sprawdza czy zamówienie i kawa zastały zakupione.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     */
    private static void addOpinion(Scanner scanner) {
        try {
            System.out.println("Podaj id zamówienia:");
            int idOrder = scanner.nextInt();
            System.out.println("Podaj id kawy:");
            int idCoffie = scanner.nextInt();
            System.out.println("Podaj ocenę kawy (1-5):");
            int scoreOrder = scanner.nextInt();
            scanner.nextLine();
            System.out.println("Podaj opinię na temat kawy:");
            String commentOrder = scanner.nextLine();
            PreparedStatement statement1 = connection.prepareStatement("SELECT * FROM ilość " +
                    " WHERE Zamowienie_id = ? AND Kawa_id = ? ");
            statement1.setInt(1, idOrder);
            statement1.setInt(2, idCoffie);
            ResultSet resultSet = statement1.executeQuery();
            if (!resultSet.next()) {
                System.out.println("Wprowadzono niewłaściwe informacje o zamówniu, spróbuj ponownie.");
                loggedInloop(scanner);
            }
            PreparedStatement statement2 = connection.prepareStatement("UPDATE ilość SET opinia = ? WHERE " +
                    "Zamowienie_id = ? AND Kawa_id = ?;");
            statement2.setString(1, commentOrder);
            statement2.setInt(2, idOrder);
            statement2.setInt(3, idCoffie);
            PreparedStatement statement3 = connection.prepareStatement("UPDATE ilość SET ocena = ? WHERE " +
                    "Zamowienie_id = ? AND Kawa_id = ?;");
            statement3.setInt(1, scoreOrder);
            statement3.setInt(2, idOrder);
            statement3.setInt(3, idCoffie);
            statement2.executeUpdate();
            statement3.executeUpdate();
            System.out.println("Poprawnie dodano opinię");
            loggedInloop(scanner);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InputMismatchException e2) {
            System.out.println("Wprowadzono niewłaściwy typ danych spróbuj ponownie.");
            loggedInloop(scanner);
        }
    }

    /**
     * Metoda pobiera input od użytkownika i sprawdza, czy zgadza się on z którymś z elementów tablicy
     * inputs. Jeżeli nie, to metoda jest wywoływana ponownie aż do momentu, w którym użytkownik wprowadzi
     * jedną z oczekiwanych wartości. Gdy w końcu otrzymamy właściwy input, to program go zwraca.
     *
     * @param scanner skaner wykorzystywany do odczytu wartości wprowadzanych przez użytkownika
     * @param inputs  String[] tablica zawierająca dostępne do wyboru opcje
     * @param message String wiadomość, która ma zostać wyświetlona przy każdym wywołaniu pętli
     *                ("" jeżeli ma jej nie być)
     * @return String input użytkownika w postaci zmiennej typu String
     */
    private static String waitForInput(Scanner scanner, String[] inputs, String message) {
        String input = scanner.nextLine();
        for (String input1 : inputs) {
            if (input1.equals(input)) {
                return input;
            }
        }
        while (true) {
            if (!"".equals(message)) {
                System.out.println(message);
            }
            input = scanner.nextLine();
            if ("home".equals(input)) {
                logIn(scanner);
            }
            for (String input1 : inputs) {
                if (input1.equals(input)) {
                    return input;
                }
            }
        }
    }
}
