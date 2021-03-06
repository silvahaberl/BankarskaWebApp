package bankaccount.web.admin;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import bankaccount.service.AccountService;
import bankaccount.service.AdminService;
import bankaccount.service.ClientService;
import bankaccount.service.TransactionService;
import bankaccount.web.TransactionController;
import bankaccount.web.TransactionDto;
import bankaccount.model.Account;
import bankaccount.model.Client;
import bankaccount.model.Transaction;

@Controller
@RequestMapping("/admin")
public class TransactionDetailsController {
	
    private static final String TRANS_DETAILS = "admin_details";
    private static final String TRANS_ALL = "admin_all";
 
    private static Logger logger = LoggerFactory.getLogger(TransactionDetailsController.class);
    
    @Autowired
    private TransactionService transactionService; 
    
    @Autowired
    private ClientService clientService; 
    
    @Autowired
    private AdminService adminService; 
	
    @RequestMapping("/all")
    public String all(Model model) {
    	
    	 
        List<Transaction> transactionsAllByAdminZadan = new ArrayList<Transaction>();
        List<Transaction> transactionsAllByAdminIzvrsen = new ArrayList<Transaction>(); 
        List<Transaction> transactionsAllByAdminOdbijen = new ArrayList<Transaction>(); 
        
        long admin = (long)1000000000;
        
        transactionsAllByAdminZadan = transactionService.findAllByStatus(admin,"zadan");
        transactionsAllByAdminIzvrsen = transactionService.findAllByStatus(admin,"izvrsen");
        transactionsAllByAdminOdbijen = transactionService.findAllByStatus(admin,"odbijen");
        /*Drop-Down list*/
        model.addAttribute("transactionsZadan",  transactionsAllByAdminZadan);
        model.addAttribute("transactionsIzvrsen",transactionsAllByAdminIzvrsen);
        model.addAttribute("transactionsOdbijen", transactionsAllByAdminOdbijen);
        logger.info("transactionsAllByAdminZadan" + transactionsAllByAdminZadan);
        logger.info("transactionService.findAllByStatus(status)" + transactionsAllByAdminZadan);
   
        return TRANS_ALL;
    }
    
    @RequestMapping("/all/{id}")
    public String details(@PathVariable("id") Integer id, Model model) {
    	
    	Transaction trans = transactionService.findById(id);
    	
    	model.addAttribute("id",trans.getId());
    	model.addAttribute("sourceIban",trans.getSourceAccount().getIban());
	    model.addAttribute("destinationIban", trans.getDestinationIban());
        model.addAttribute("balance", trans.getBalance());
        model.addAttribute("status", trans.getStatus());
        model.addAttribute("time", trans.getCurrentTime());
        model.addAttribute("verified", trans.getVerified());
 
        return TRANS_DETAILS;
    }
    
    @RequestMapping(value = "/all/{id}/izvrsi")
    public String izvrsi(@PathVariable("id") Integer id, Model model) {
    	
    	boolean success = false;
    	
    	if (transactionService.findById(id) != null) {
    		
	    	success = true;
	    	
	    	Transaction trans = transactionService.findById(id);
	    	
	    	if(adminService.checkBalance(trans) == false) {
	    	
	    		trans.setStatus("izvrsen");
            
	    		adminService.executeAdmin(trans);
	    	}	
	    	else{ 
	    		
	    		trans.setStatus("odbijen");	
	    		transactionService.save(trans);
	    	}
	    	
	    	return "redirect:/admin/all";
    	}
    	else 
    		model.addAttribute("isSucess", success);
    
 
        return TRANS_DETAILS;
    }
    
    @RequestMapping(value = "/all/{id}/odbij")
    public String odbij(@PathVariable("id") Integer id, Model model) {
    	
    	boolean success = false;
    	
    	if (transactionService.findById(id) != null) {
    		
	    	success = true;
	    	
	    	Transaction trans = transactionService.findById(id);
	    	
	    	trans.setStatus("odbijen");
	    	
	    	transactionService.save(trans);
	    	
    		return "redirect:/admin/all";
    	}
    	else 
    		model.addAttribute("isSucess", success);
    
 
        return TRANS_DETAILS;
    }

}
