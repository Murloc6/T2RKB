/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ThesaurusTreatment;

import Disambiguator.WordNetDesambiguisator;
import java.util.ArrayList;
import java.util.HashMap;
import net.didion.jwnl.JWNLException;
import net.sf.json.JSONObject;
import t2kb.SparqlProxy;

/**
 *
 * @author murloc
 */
public class ThesaurusProcess 
{
    private SparqlProxy spIn;
    private SparqlProxy spOut;
    
    
    public ThesaurusProcess(String urlServerIn, String urlServerOut)
    {
        this.spIn = SparqlProxy.getSparqlProxy(urlServerIn);
        this.spOut = SparqlProxy.getSparqlProxy(urlServerOut);
        System.out.println("Clearing ouput KB...");
        this.spOut.storeData(new StringBuilder("DELETE WHERE {?a ?b ?c}")); // clean the output sparql endpoint before adding contents
    }
    
    public void loadAllConcepts()
    {
        System.out.println("Initialize WordNet ...");
        WordNetDesambiguisator wnd = WordNetDesambiguisator.getWordNetConnector();
        System.out.println("WordNet initialized!");
        
        ArrayList<JSONObject> listURI = this.spIn.sendQuery("SELECT * WHERE{?c rdf:type skos:Concept.}");
        System.out.println(listURI.size()+" concepts  import");
        System.out.println("Begin creating raw KB ...");
        int i = 0;
        int nbQuery = 0;
        int nbSubClassOf = 0;
        int nbSubClassOfValidated = 0;
        int nbSubClassOfNotFoundWN = 0;
        int nbSubClassOfNotValidated = 0;
        
        // def relations
        HashMap<String, OntologyRelation> objProp = new HashMap<>();
        
        StringBuilder updateQuery = new StringBuilder("INSERT DATA {");
        
        for(JSONObject s : listURI)
        {
            
            String currentQueryPart = "";
            String currentObjPropQuerypart = "";
            
            String uri = s.getJSONObject("c").getString("value");
            currentQueryPart += "<"+uri+"> rdf:type owl:Class; ";
            
            ArrayList<JSONObject> listRel = this.spIn.sendQuery("SELECT * WHERE {<"+uri+"> ?rel ?val. }");
            for(JSONObject sRel : listRel)
            {
                String rel = sRel.getJSONObject("rel").getString("value");
                String val = sRel.getJSONObject("val").getString("value");
                if(SparqlProxy.isLabelRel(rel))
                {
                    String lang = sRel.getJSONObject("val").getString("xml:lang");
                    String label = SparqlProxy.cleanString(val);
                    if(!label.isEmpty())
                    {
                        if(lang.isEmpty())
                            currentQueryPart += " rdfs:label \""+SparqlProxy.cleanString(val)+"\";";
                        else
                            currentQueryPart += " rdfs:label \""+SparqlProxy.cleanString(val)+"\"@"+lang+";";
                    }
                }
                else if(SparqlProxy.isSubRel(rel))
                {
                    nbSubClassOf ++;
                    Boolean isSubClassOf = this.isSubClassOf(wnd, uri, val);
                    if(isSubClassOf == null || isSubClassOf)
                    {
                        currentQueryPart += " rdfs:subClassOf  <"+val+">; ";
                        if(isSubClassOf == null)
                        {
                            nbSubClassOfNotFoundWN++;
                        }
                        else if(isSubClassOf)
                        {
                            nbSubClassOfValidated ++;
                        }
                    }
                    else
                    {
                        nbSubClassOfNotValidated++;
                    }
                }
                else if(!(SparqlProxy.isExcludeRel(rel)) && val.startsWith("http://aims.fao.org/aos/agrovoc/"))
                {
                    //currentQueryPart += " <"+rel+">  <"+val+">;";
                    /* TODO stock all the domains and ranges of objectproperties*/
                    OntologyRelation or = objProp.get(rel);
                    if(or == null)
                    {
                        or = new OntologyRelation(rel, this.spOut);
                        objProp.put(rel, or);
                    }
                    String subProp = or.addRangeDomain(uri, val);
                    currentObjPropQuerypart += subProp;
                }
                
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
        
        System.out.println("------------------");
        System.out.println("Nb subClassOf discovered :  "+nbSubClassOf);
        System.out.println("Nb subClassOf validated by WN : "+nbSubClassOfValidated);
        System.out.println("Nb subClassOf not found in WN : "+nbSubClassOfNotFoundWN);
        System.out.println("Nb subClassOf not validated by WN : "+nbSubClassOfNotValidated);
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
    
}
