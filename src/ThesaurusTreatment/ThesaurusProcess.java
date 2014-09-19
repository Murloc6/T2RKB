/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ThesaurusTreatment;

import Disambiguator.WordNetDesambiguisator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.didion.jwnl.JWNLException;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import t2kb.SparqlProxy;

/**
 *
 * @author murloc
 */
public class ThesaurusProcess 
{
    private SparqlProxy spIn;
    private SparqlProxy spOut;
    
    HashMap<String, String> ranksAlign;
    
    String adomFileName;
    
    public ThesaurusProcess(String urlServerIn, String urlServerOut, String adomFileName)
    {
        ranksAlign = new HashMap<>();
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_330935", "http://ontology.irstea.fr/AgronomicTaxon#ClassRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_330937", "http://ontology.irstea.fr/AgronomicTaxon#FamilyRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_11125", "http://ontology.irstea.fr/AgronomicTaxon#GenusRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_330934", "http://ontology.irstea.fr/AgronomicTaxon#KingdomRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_330936", "http://ontology.irstea.fr/AgronomicTaxon#OrderRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_330950", "http://ontology.irstea.fr/AgronomicTaxon#PhylumRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_331243", "http://ontology.irstea.fr/AgronomicTaxon#SpecyRank");
        ranksAlign.put("http://aims.fao.org/aos/agrovoc/c_8157", "http://ontology.irstea.fr/AgronomicTaxon#VarietyRank");
        
        this.adomFileName = adomFileName;
        
        this.spIn = SparqlProxy.getSparqlProxy(urlServerIn);
        this.spOut = SparqlProxy.getSparqlProxy(urlServerOut);
        System.out.println("Clearing ouput KB...");
        this.spOut.storeData(new StringBuilder("DELETE WHERE {?a ?b ?c}")); // clean the output sparql endpoint before adding contents
    }
    
    
    public String getADOMTtl()
    {
        
//        String ret = "prefix : <http://www.w3.org/2002/07/owl#> \n" +
//"prefix owl: <http://www.w3.org/2002/07/owl#> \n" +
//"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//"prefix xml: <http://www.w3.org/XML/1998/namespace> \n" +
//"prefix xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
//"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
        
        String ret = "prefix : <http://www.w3.org/2002/07/owl#> \n" +
"prefix owl: <http://www.w3.org/2002/07/owl#> \n" +
"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
"prefix xml: <http://www.w3.org/XML/1998/namespace> \n" +
"prefix xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
"prefix skos: <http://www.w3.org/2004/02/skos/core#> \n" +
"prefix swrl: <http://www.w3.org/2003/11/swrl#> \n" +
"prefix swrlb: <http://www.w3.org/2003/11/swrlb#> \n" +
"prefix terms: <http://purl.org/dc/terms/> \n" +
"prefix AgronomicTaxon: <http://ontology.irstea.fr/AgronomicTaxon#> \n" +
"base <http://ontology.irstea.fr/AgronomicTaxon> \n";
        
        ret += "INSERT DATA {";
        
        try 
        {
            ret +=  IOUtils.toString( new FileInputStream(new File(this.adomFileName)));
            //ret = ret.replaceAll("^@.+\\.$", "");   // remove Prefix (wrong syntax for SPARQL insert query)
        } 
        catch (IOException ex) 
        {
            System.err.println("Can't read adom file!");
            System.exit(0);
        }
        
        
        ret += "}";
        return ret;
    }
    
    
    public boolean isAcceptLang(String lang)
    {
        return ((lang.compareToIgnoreCase("en") == 0) || (lang.compareToIgnoreCase("fr") == 0) || (lang.compareToIgnoreCase("de") == 0) || (lang.compareToIgnoreCase("es") == 0));
    }
    
    public void loadAllConcepts(String limitUri)
    {
        /*System.out.println("Initialize WordNet ...");
        WordNetDesambiguisator wnd = WordNetDesambiguisator.getWordNetConnector();
        System.out.println("WordNet initialized!");*/
        
        System.out.println("Exporting ADOM ...");
        if(!this.spOut.storeData(new StringBuilder(this.getADOMTtl()), false))
        {
            System.out.println("ERROR during exporting ADOM");
            System.exit(0);
        }
        System.out.println("ADOM exported!");
        
        //ArrayList<JSONObject> listURI = this.spIn.sendQuery("SELECT * WHERE{?c rdf:type skos:Concept. ?c skos:broader+ <http://aims.fao.org/aos/agrovoc/c_5993>.}"); //toutes les plantes
        ArrayList<JSONObject> listURI = this.spIn.sendQuery("SELECT * WHERE{?c rdf:type skos:Concept. ?c skos:broader+ <"+limitUri+">.}");
        listURI.addAll(this.spIn.sendQuery("SELECT * WHERE {?c rdf:type skos:Concept.  <"+limitUri+"> skos:broader+ ?c.}"));
        JSONObject joMain = new JSONObject();
        JSONObject cUri = new JSONObject();
        cUri.put("value", limitUri);
        joMain.put("c", cUri);
        System.out.println(joMain);
        listURI.add(joMain);
        System.out.println(listURI.size()+" concepts  import");
        System.out.println("Begin creating raw KB ...");
        int i = 0;
        int nbQuery = 0;
        int nbSubClassOf = 0;
        
        String baseUri = "http://ontology.irstea.fr/AGROVOCTaxon#";
        
        //String stringStats = " Uri sujet ; label français ; relation ; WordNet; Uri objet ; label français ; Validation Catherine; Wikipedia; NCBI; note";
        
        // def relations
        HashMap<String, OntologyRelation> objProp = new HashMap<>();
        
        StringBuilder updateQuery = new StringBuilder("INSERT DATA {");
        
        String classifUri = baseUri+"classification_AGROVOC";
        
        updateQuery.append( "<"+classifUri+"> rdf:type <http://ontology.irstea.fr/AgronomicTaxon#Taxonomy>; rdfs:label \"AGROVOC agronomic classification\".");
        
        for(JSONObject s : listURI)
        {
            
            String currentQueryPart = "";
            String currentObjPropQuerypart = "";
            
            String uri = s.getJSONObject("c").getString("value");
            
            String rankUri = "http://ontology.irstea.fr/AgronomicTaxon#Taxon";
            ArrayList<JSONObject> elemRanks = this.spIn.sendQuery("SELECT * WHERE {<"+uri+"> agrovoc:hasTaxonomicLevel ?rankUri.}");
            if(!elemRanks.isEmpty())
            {
                String uriInRank = elemRanks.get(0).getJSONObject("rankUri").getString("value");
                rankUri = this.ranksAlign.get(uriInRank);
                if(rankUri == null)
                {
                    //rankUri = "http://ontology.irstea.fr/AgronomicTaxon#Taxon";
                    currentQueryPart += "<"+uriInRank+"> rdf:type owl:Class; rdfs:subClassOf  <http://ontology.irstea.fr/AgronomicTaxon#Taxon>;  ";
                    ArrayList<JSONObject> labelsClass = this.spIn.sendQuery("SELECT * WHERE {<"+uriInRank+">  (<http://www.w3.org/2004/02/skos/core#prefLabel>|<http://www.w3.org/2004/02/skos/core#altLabel>) ?label}");
                    for(JSONObject sLabel : labelsClass)
                    {
                        String lang = sLabel.getJSONObject("label").getString("xml:lang");
                        String label = SparqlProxy.cleanString(sLabel.getJSONObject("label").getString("value"));
                        //currentQueryPart += " rdfs:label ";
                        if(lang.isEmpty())
                        {
                            currentQueryPart += " rdfs:label  \" "+label+"\"; ";
                        }
                        else if(this.isAcceptLang(lang))
                        {
                            currentQueryPart += " rdfs:label  \""+label+"\"@"+lang+"; ";
                        }
                    }
                    
                    currentQueryPart = currentQueryPart.substring(0, currentQueryPart.lastIndexOf(";"));
                    currentQueryPart += ".";
                    rankUri = uriInRank;
                }
            }
            
            currentQueryPart += "<"+uri+"> rdf:type <"+rankUri+">; <http://ontology.irstea.fr/AgronomicTaxon#inScheme> <"+classifUri+">; ";
            
            ArrayList<JSONObject> listRel = this.spIn.sendQuery("SELECT * WHERE {<"+uri+"> ?rel ?val.}");
            for(JSONObject sRel : listRel)
            {
                String rel = sRel.getJSONObject("rel").getString("value");
                String val = sRel.getJSONObject("val").getString("value");
                if(SparqlProxy.isLabelRel(rel))
                {
                    String lang = sRel.getJSONObject("val").getString("xml:lang");
                    String label = SparqlProxy.cleanString(val);
                    String adomRelUri = "http://ontology.irstea.fr/AgronomicTaxon#hasVernacularName";
                    if(rel.equalsIgnoreCase("http://www.w3.org/2004/02/skos/core#prefLabel"))
                    {
                        adomRelUri = "http://ontology.irstea.fr/AgronomicTaxon#hasScientificName";
                    }
                    
                    if(!label.isEmpty())
                    {
                        if(lang.isEmpty())
                        {
                            currentQueryPart += " <"+adomRelUri+"> \""+SparqlProxy.cleanString(val)+"\";";
                            currentQueryPart += " rdfs:label  \""+SparqlProxy.cleanString(val)+"\";";
                        }
                        else if (this.isAcceptLang(lang))
                        {
                            currentQueryPart += " <"+adomRelUri+"> \""+SparqlProxy.cleanString(val)+"\"@"+lang+";";
                            currentQueryPart += " rdfs:label \""+SparqlProxy.cleanString(val)+"\"@"+lang+";";
                        }
                    }
                }
                else if(SparqlProxy.isSubRel(rel))
                {
                    nbSubClassOf ++;
                    currentQueryPart += " <http://ontology.irstea.fr/AgronomicTaxon#hasHigherRank>   <"+val+">; ";
                    /* REMOVE WordNet desambiguator
                    Boolean isSubClassOf = this.isSubClassOf(wnd, uri, val);
                    if(isSubClassOf == null || isSubClassOf)
                    {
                        currentQueryPart += " rdfs:subClassOf  <"+val+">; ";
                        if(isSubClassOf == null)
                        {
                            nbSubClassOfNotFoundWN++;
                            stringStats += "\n"+this.getConceptsStatsDescription(uri, val, "Not found");
                        }
                        else if(isSubClassOf)
                        {
                            nbSubClassOfValidated ++;
                            stringStats += "\n"+this.getConceptsStatsDescription(uri, val, "Validated");
                        }
                    }
                    else
                    {
                        nbSubClassOfNotValidated++;
                        stringStats += "\n"+this.getConceptsStatsDescription(uri, val, "Not validated");
                    }*/
                }
                /*else if(!(SparqlProxy.isExcludeRel(rel)) && val.startsWith("http://aims.fao.org/aos/agrovoc/"))
                {
                    OntologyRelation or = objProp.get(rel);
                    if(or == null)
                    {
                        or = new OntologyRelation(rel, this.spOut);
                        objProp.put(rel, or);
                    }
                    String subProp = or.addRangeDomain(uri, val);
                    currentObjPropQuerypart += subProp;
                }*/
                
            }
            currentQueryPart = currentQueryPart.substring(0, currentQueryPart.lastIndexOf(";"));
            currentQueryPart += ".";
            currentQueryPart += currentObjPropQuerypart;
            
            int fullLength = updateQuery.length()+currentQueryPart.length();
            if(fullLength > 4000000) // limit for the fuseki update query length
            {
                nbQuery ++;
                updateQuery.append("}");
                boolean ret = this.spOut.storeData(updateQuery);
                System.out.println(i+" concepts treated (query n° "+nbQuery+")...");
                if(!ret) //if store query bugged
                    System.exit(0);
                updateQuery = new StringBuilder("INSERT DATA {"+currentQueryPart);
            }
            else
            {
                updateQuery.append(currentQueryPart);
            }
            i++;
        }
        
        if(!updateQuery.equals("INSERT DATA {"))
        {
            nbQuery ++;
            updateQuery.append("}");
            this.spOut.storeData(updateQuery);
            System.out.println(i+" concepts treated (query n° "+nbQuery+")...");
        }
        
        
        
        System.out.println("Begin object properties definitions ("+objProp.values().size()+" objProps) ...");
        int nbObjProp = 0;
        nbQuery = 0;
        int nbObjPropCached = 0;
        updateQuery = new StringBuilder("INSERT DATA {");
        
        for(OntologyRelation or : objProp.values())
        {
            StringBuilder currentQueryPart = new StringBuilder("");
            nbObjProp ++;
            currentQueryPart.append(or.toTtl(this.spOut));
            //currentQueryPart.append(or.getSubPropertiesTtl());
            
            int fullLength = updateQuery.length()+currentQueryPart.length();
            if(fullLength > 4000000) // limit for the fuseki limit POST request
            {
                nbQuery ++;
                updateQuery.append("}");
                boolean ret = this.spOut.storeData(updateQuery);
                System.out.println(nbObjProp+" properties treated (query n° "+nbQuery+")...");
                if(!ret) //if store query bugged
                    System.exit(0);
                updateQuery = new StringBuilder("INSERT DATA {"+currentQueryPart);
                nbObjPropCached = 0;
            }
            else
            {
                updateQuery.append(currentQueryPart);
            }
            nbObjPropCached++;
            System.out.println(nbObjPropCached+" Object properties definitions cached");
        }
        if(!updateQuery.equals("INSERT DATA {"))
        {
            nbQuery ++;
            updateQuery.append("}");
            this.spOut.storeData(updateQuery);
            System.out.println(nbObjProp+" properties treated (query n° "+nbQuery+")...");
        }
        
        
        
        System.out.println("Raw KB ("+i+" concepts || "+nbObjProp+" objProps) created!");
        
      /*  System.out.println("------------------ \n");
        String s = "Nb subClassOf discovered :  "+nbSubClassOf+" \n";
        s += "Nb subClassOf validated by WN : "+nbSubClassOfValidated+"\n";
        s += "Nb subClassOf not found in WN : "+nbSubClassOfNotFoundWN+"\n";
        s += "Nb subClassOf not validated by WN : "+nbSubClassOfNotValidated+"\n";
        System.out.println(s);
        File fStatsOut = new File("out/Stats_out.txt");
        File fileStat = new File("out/stats_WordNet.csv");
        try 
        {
            FileUtils.writeStringToFile(fStatsOut, s, "UTF8", false);
            FileUtils.writeStringToFile(fileStat, stringStats);
        } 
        catch (IOException ex) 
        {
            System.out.println("EROR during output stats out file ");
            System.exit(0);
        }*/
        
        
    }
 
    /* SPARQL query save (to facilitate copy/paste)
    SELECT * WHERE {?a ?b ?c} LIMIT 10
    DELETE {?a ?b ?c} WHERE {?a ?b ?c}
    
  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX owl:    <http://www.w3.org/2002/07/owl#>
SELECT COUNT(?c) WHERE{?c rdf:type owl:Class.}
    
    
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX owl:    <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
ASK {<http://aims.fao.org/aos/agrovoc/c_7955> rdfs:subClassOf* <http://aims.fao.org/aos/agrovoc/c_3354>.}
    
    */
    
    public Boolean isSubClassOf(WordNetDesambiguisator wnd, String uri1, String uri2)
    {
        Boolean ret = null;
        ArrayList<String> labelsUri1 = new ArrayList<>();
        ArrayList<String> labelsUri2 = new ArrayList<>();
        
        
        ArrayList<JSONObject> retLabels = this.spIn.sendQuery("SELECT * WHERE {<"+uri1+"> skos:prefLabel ?label .   FILTER( LANGMATCHES(LANG(?label), \"en\")). }");
        for(JSONObject jsono : retLabels)
        {
            labelsUri1.add(jsono.getJSONObject("label").getString("value"));
        }
        
        retLabels = this.spIn.sendQuery("SELECT * WHERE {<"+uri2+"> skos:prefLabel ?label .   FILTER( LANGMATCHES(LANG(?label), \"en\")). }");
        for(JSONObject jsono : retLabels)
        {
            labelsUri2.add(jsono.getJSONObject("label").getString("value"));
        }
        //System.out.println("INIT CARTESIAN : "+labelsUri1.size()+" X "+labelsUri2.size());
        int i =0, j=0;
        //Cartesian product to test all english terms couple
        while( (ret == null  ||  !ret) && i<labelsUri1.size())
        {
            String term1 = labelsUri1.get(i);
            if(wnd.isInWordNet(term1))
            {
                while((ret == null || !ret) && j<labelsUri2.size())
                {
                    String term2 = labelsUri2.get(j);
                    if(wnd.isInWordNet(term2))
                    {
                        ret = wnd.getRelation(term1, term2).equalsIgnoreCase("subClassOf");
                        //System.out.println("relation between : "+term1+" ---"+ret+"---> "+term2);
                    }
                    else
                    {
                        //System.out.println("Term not found in WN (2): "+term2);
                    }
                    j++;
                }
            }
            else
            {
                //System.out.println("Term not found in WN : "+term1);
            }
            i++;
        }
        
        //System.out.println("Return : "+ret);
        return ret;
    }
    
    public void exportKBToFile(String fileName)
    {
        this.spOut.writeKBFile(fileName);
    }
    
    
    private ArrayList<String> getAllLabel(String uri, String lang)
    {
        ArrayList<String> ret = new ArrayList<>();
        
        String query = "SELECT * WHERE { <"+uri+"> (skos:prefLabel | skos:altLabel) ?label.";
        if(lang != null)
        {
            query += "FILTER ( lang(?label) = \"fr\" )";
        }
        query += "}";
        ArrayList<JSONObject> labels = this.spIn.sendQuery(query);
        for(JSONObject  jsono : labels)
        {
            ret.add(jsono.getJSONObject("label").getString("value"));
        }
        return ret;
    }
    
    private ArrayList<String> getAllLabelsLang(String uri)
    {
        ArrayList<String>sLabels = this.getAllLabel(uri, "fr");
        if(sLabels.isEmpty())
        {
            sLabels = this.getAllLabel(uri, "en");
        }
        if(sLabels.isEmpty())
        {
            sLabels = this.getAllLabel(uri, null);
        }
        return sLabels;
    }
    
    private String getConceptsStatsDescription(String uriS, String uriO, String status)
    {
        String ret = "";
        
        String subjectLabels = "";
        for(String s : this.getAllLabelsLang(uriS))
        {
            subjectLabels += s+", ";
        }
        
        String objectLabels = "";
        for(String s : this.getAllLabelsLang(uriO))
        {
            objectLabels += s+", ";
        }
        
         ret  = uriS+" ; "+subjectLabels+"; subClassOf ; "+status+"; "+uriO+"; "+objectLabels+"; ; ; ; ";
        
        return ret;
    }
    
}
