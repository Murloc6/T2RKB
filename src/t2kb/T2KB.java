/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package t2kb;

import ThesaurusTreatment.ThesaurusProcess;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author murloc
 */
public class T2KB {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
       ThesaurusProcess tp = new ThesaurusProcess("http://localhost:3030/AGROVOC-SKOS/", "http://localhost:3030/AGROVOC_OWL_OUT/");
       System.out.println("Begin thesaurus treatment ...");
        tp.loadAllConcepts();
        System.out.println("thesaurus treated!");
        
        String dateFileName = new SimpleDateFormat("dd-MM_HH:mm_||_").format(new Date());
        
        System.out.println("Exporting the KB to the file out/"+dateFileName+"_AGROVOC_OWL.owl ...");
        tp.exportKBToFile(dateFileName+"_AGROVOC_OWL");
        System.out.println("KB exported!");
        
    }
    
}
