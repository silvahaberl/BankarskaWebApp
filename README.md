# BankarskaWebApp

Za kreiranje korisničkog računa

    public Client(String username, String password) {
    	
        this.username = username;
        this.password = PasswordService.getPasswordHash(password);
        
        this.account = new Account();
    }
    
koristim klasu Client, sa svim potrebnim getterima i setterima za svojstva klijenta: id (automatski se generira) , username, password tipa String (password je kriptiran u heksadecimalan broj, metodom sha1hex u Util klasi) ,te tip Account (pripadni korisnički račun koji mu je automatski dodjeljen na način da se instancira pozivom konstruktora objekta tipa Client) implementiran klasom Account. 


Account ima inicijalnu vrijednost 1000 novaca

    public Account() {
    	
        this.iban = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        this.balance = 1000;
    }
    
jer mu je u konstruktoru balance svojstvo inicijalizirano na 1000, a svojstvo iban je broj tipa long , duljine 10 znamenaka koristeći funkciju random() koja bira nasumično prirodne brojeve, a množeći ju sa velikim brojem određene duljine dobivamo duljinu broja od 10 znamenaka.


Početna stranica aplikacije ostvarena template-om *index.html* nudi tri linka: kreiranje klijenta, login i napravi transakciju.
Korištenje elemenata bootstrapa kroz ostale templeate ostvarujem tako da includam Bootstrap u *index.html*, a datoteke Javascripta i Bootstrapa se nalaze u static folderu projekta.


**NewClientController** označen notacijom *@Controller* povezan je s rutom */clientcreate* uz notaciju *RequestMapping* i koristi metode *ClientService*-a korištenjem notacije *@Autowired* prije deklaracije servisa.


    @Autowired
    public ClientService service; 
   
    @RequestMapping(method = RequestMethod.GET)
    public String clientCreate( Model model) {
    	
        model.addAttribute("clientInfo", new ClientDto());
      
      
        return CREATE_VIEW;
    }
    
    
Get metoda *clientCreate* kreira objekt *ClientDto* za prijenos (DTO- *Data transfer object*) podataka novostvorenog klijenta (username i password) i vraća *view* kako bi korisnik unio svoje podatke u formu i otvorio račun pritiskom gumba. 
Post metoda *create* podatke iz forme na pritisak gumba prosljeđuje podatke iz *ClientDto* objekta u kontruktor Client objekta kako bi se generirao novi korisnik i njegov račun (ako već korisnik ne postoji u bazi, a to provjerim metodom servisa ClientService, *findByUsername* koja vrati različito od *null* ako postoji osoba s tim username-om u bazi). Spremanje korisnika u bazu radim metodom *save* servisa *ClientService*. U bazi se nalazi generirana tablica *Client* koja ima vezu jedan klijent sa jednim računom (svojstvo account u Client objektu sa id-jem u Account objektu).

**LoginController** 

1. *Autentifikacija*

povezan je rutom */login* . Metoda *loginForm* prosljeđuje objekt *ClientDto* view-u *client_login*. 
	   
 @RequestMapping(method = RequestMethod.POST)
    public String login(@ModelAttribute("clientInfo")ClientDto clientInfo, Model model,RedirectAttributes redirectAttributes) {


        if(service.isValid(clientInfo.getUsername(), clientInfo.getPassword())) {

            redirectAttributes.addFlashAttribute("clientInfo", clientInfo);
            
            if(clientInfo.getRole() == Role.ROLE_ADMIN){
            	
            	return "redirect:/admin/all";
            	
            }
            
            return "redirect:/transactioncreate";
        }

        model.addAttribute("isWrongPassword", true);


        return loginForm(model);
    }


Formom se šalju podaci post metodom *login* , te korištenjem metode *isValid* servisa ClientService ispitujem: stvorene podatke klijenta, je li forma bila popunjena i postoji li u bazi klijent, metodom *findByUsername*. Ukoliko klijent već postoji, odrađujem login na način da njegove podatke šaljem objektom *redirectAttributes* na rutu */transactioncreate*. 
AKo je klijent zapravo *admin*, odnosno klijent sa username/password banka/banka, radim redirect na rutu */admin/all* za ispis svih postojećih transakcija u sustavu.
Inače popunjavam objekt *model* sa informacijom da je lozinka kriva, te tu informaciju ispisujem u viewu *client_create*.


2. *Autorizacija*

Za veću sigurnost *url-a , metoda i podataka * možemo mijenjati metodu *configure* u klasi *WebSecurityConfigurerAdapter*
  
    @Override
    protected void configure(HttpSecurity http) throws Exception {
    	
        http
            .authorizeRequests()
            .antMatchers("/").permitAll()
            .antMatchers("/login").permitAll()
            .antMatchers("/clientcreate").permitAll();
        http.csrf().disable();
        http.headers().frameOptions().disable();
    }
    
te napraviti promjene u kodu ispred metoda sa notacijom *@PreAuthorize* ili *@Secure* ako želimo odrediti koji korisnik će ih moći koristiti: USER ili ADMIN.

**TransactionController**

povezan rutom */transactioncreate*. 
Podaci koji stižu objektom redirectAttributes na ovu rutu su podaci o logiranom klijentu (kao parametri metode *Form* , objekt *clientInfo*). Pomoću imena klijenta pretražujem bazu podataka da bi dobila odgovarajući IBAN računa. To je moguće jer je klijentovo ime jedinstveno na nivou sustava. Spremam IBAN računa za prikaz u viewu.
Kreiram novi objekt *TransactionDto* koji će služiti u prijenosu podataka iz forme transakcijskog naloga do kreiranja njegovog objekta čije informacije mogu pospremiti u bazu. Te informacije su *properties* objekta *Transaction*: id, sourceAccount, destinationIban, status (zadan,odbijen,izvrsen), balance, time, verified.
*sourceAccount* tipa Account je svojstvo po kojem prepoznajemo vlasnika klijenta i veza sa tablicom Account.Ta veza je veza tipa: mnogo transakcija sa jednim računom korisnika (Account id-jem).

Metoda *transaction* stvara novu transakciju sa statusom *zadan* i sprema ju u bazu u tablicu *Transaction*, ako je ispunjen uvjet da je iznos transakcije manji od kolicine novca na trenutnom racunu ispitanog u if uvjetu:


	if (service.findBalanceByIban(transactionInfo.getSourceIban()) < transactionInfo.getAmount()) 
			return "error";

Informacije o transakciji iz forme su omogućile putem servisa dohvaćanje IBANA trenutnog klijenta i iznosa novaca transakcije.
Servis *TransactionServis* omogućuje spremanje transakcije u tablicu, a zatim putem objekta redirectAttribute informacije o transakciji prosljeđujemo ruti *list*.

Metoda *listTransactions* dohvaća sve transakcije trenuntog korisnika, posprema ih u tri liste po statusima: *zadan,odbijen i izvsen*. Ti podaci su dostupni u viewu *transaction_list*. 

    	
	 @RequestMapping(value = "list", method = RequestMethod.GET)
	    public String listTransactions(@ModelAttribute("transactionInfo")TransactionDto transactionInfo, Model model) {
		     	
	    	long currentIban = transactionInfo.getSourceIban();    	
	        model.addAttribute("transactionInfo", currentIban);
	           
	        List<Transaction> transactionsMyIbanZadan = new ArrayList<Transaction>();
	        List<Transaction> transactionsMyIbanIzvrsen = new ArrayList<Transaction>(); 
	        List<Transaction> transactionsMyIbanOdbijen = new ArrayList<Transaction>(); 
	        
	        transactionsMyIbanZadan = serviceT.findAllByStatus(currentIban,"zadan");
	        transactionsMyIbanIzvrsen = serviceT.findAllByStatus(currentIban,"izvrsen");
	        transactionsMyIbanOdbijen = serviceT.findAllByStatus(currentIban,"odbijen");
	        /*Drop-Down list*/
	        model.addAttribute("transactionsZadan",  transactionsMyIbanZadan);
	        model.addAttribute("transactionsIzvrsen",transactionsMyIbanIzvrsen);
	        model.addAttribute("transactionsOdbijen", transactionsMyIbanOdbijen);
	 
	        
	        return TRANS_LIST;
	    }
	 



U template-u *transaction_list* uz pomoć javascript funkcije i html elemenata tablice i oznake *class* koja moze biti *zadan,izvrsen i odbijen*, moguće je transakcije filtrirati sa *drop-down* izbornikom *select-option*.

		<p>Filtriraj po statusu transakcije:</p>		
		<select id="trans">
		  <option value="">Odaberi status</option>
		  <option value="zadan">ZADAN</option>
		  <option value="odbijen">ODBIJEN</option>
		  <option value="izvrsen">IZVRŠEN</option>
		  </select>
		<table class ="zadan" border="1">
		     <tr id="header">
				<td>Broj računa uplate:</td> 
				<td>Iznos transakcije:</td>
				<td>Status transakcije:</td>   
		    </tr>
		    <tr th:each="t : ${transactionsZadan}">
			 <td th:text="${t.destinationIban}"></td>
			 <td th:text="${t.balance}"></td>
			 <td th:text="${t.status}"></td>
		    </tr>
		</table>
		
**TransactionDetailsController**
povezan rutom */admin* , uz pomoć metode *all* u ruti */admin/all* dohvaća iz tablice *Transaction* sve naloge metodom *transactionService-a* *findAllByStatus()* koja šalje parametre: broj računa admina (fiksno zadan u bazi od "clienta" sa user/password  banka/banka , broj tipa long, 1000000000) sve transakcije u bazi i ispisuje ih u template-u *admin_all*.

        
        long admin = (long)1000000000;
        
        transactionsAllByAdminZadan = transactionService.findAllByStatus(admin,"zadan");
        transactionsAllByAdminIzvrsen = transactionService.findAllByStatus(admin,"izvrsen");
        transactionsAllByAdminOdbijen = transactionService.findAllByStatus(admin,"odbijen");

Uz pomoć metode details, pvezan je s rutom */admin/all/{id}*. Metoda details prima kao parametar *id* pojedine transakcije kada je admin pritisnuo link za dohvaćanja detalje neke transakcije. Detalji su prikazani u template-u *admin_details*.

Za izvršavanje i odbijanje transakcija služe metode *izvrsi* i *odbij*. U njima su transakvije dohvaćene iz baza, promijenjeni im statusi i ponovno su spremljene u bazu.


**pom.xml**


Odabrala sam koristiti *Maven* za build system.
Dodala sam sve dependencies koji su bili potrebni redom kako sam radila: spring-boot-starter-data-jpa,hibernate-validator,spring-webmvc,spring-boot-starter-thymeleaf itd. 

		<dependency>
           		<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-thymeleaf</artifactId>
		</dependency>
       		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.webjars</groupId>
			<artifactId>bootstrap</artifactId>
			<version>3.3.7</version>
		</dependency>

**Screenshots**

![ekran1](https://user-images.githubusercontent.com/6881169/34145392-1ed17d10-e496-11e7-8c75-e44d055fa2cb.png)
![ekran2](https://user-images.githubusercontent.com/6881169/34145393-1ef0ed3a-e496-11e7-930e-a04e8b2f8148.png)
![ekran3](https://user-images.githubusercontent.com/6881169/34145394-1f0ee2f4-e496-11e7-85e5-e8ad9a4c8f6c.png)
![ekran4](https://user-images.githubusercontent.com/6881169/34145396-1f2bd1de-e496-11e7-9cac-19b837aee241.png)
![ekran5](https://user-images.githubusercontent.com/6881169/34145397-1f48baa6-e496-11e7-8f71-039d75cd36e6.png)
![ekran6](https://user-images.githubusercontent.com/6881169/34145525-bc92a40c-e496-11e7-994c-a1a28717d9f1.png)


