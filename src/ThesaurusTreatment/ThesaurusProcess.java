/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ThesaurusTreatment;

import java.util.ArrayList;
import java.util.HashMap;
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
    }
    
    public void loadAllConcepts()
    {
        ArrayList<JSONObject> listURI = this.spIn.sendQuery("SELECT * WHERE{?c rdf:type skos:Concept.}");
        System.out.println(listURI.size()+" concepts  import");
        System.out.println("Begin creating raw KB ...");
        int i = 0;
        int nbQuery = 0;
        
        // def relations
        HashMap<String, OntologyRelation> objProp = new HashMap<>();
        
        StringBuilder updateQuery = new StringBuilder("INSERT DATA {");
        
        for(JSONObject s : listURI)
        {
            
            String currentQueryPart = "";
            
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
                    currentQueryPart += " rdfs:subClassOf  <"+val+">; ";
                }
                else if(!(SparqlProxy.isExcludeRel(rel)) && val.startsWith("http://aims.fao.org/aos/agrovoc/"))
                {
                    currentQueryPart += " <"+rel+">  <"+val+">;";
                    /* TODO stock all the domains and ranges of objectproperties*/
                    OntologyRelation or = objProp.get(rel);
                    if(or == null)
                    {
                        or = new OntologyRelation(rel, this.spOut);
                        objProp.put(rel, or);
                    }
                    or.addDomain(uri);
                    or.addRange(val);
                }
                
            }
            currentQueryPart = currentQueryPart.substring(0, currentQueryPart.lastIndexOf(";"));
            currentQueryPart += ".";
            
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
            updateQuery.append("}");
            this.spOut.storeData(updateQuery);
        }
        
        
        
        System.out.println("Begin object properties definitions ("+objProp.values().size()+" objProps) ...");
        int nbObjProp = 0;
        updateQuery = new StringBuilder("INSERT DATA {");
        for(OntologyRelation or : objProp.values())
        {
            nbObjProp ++;
            updateQuery.append(or.toTtl());
            System.out.println(nbObjProp+" objProps treated ...");
        }
        updateQuery.append("}");
        this.spOut.storeData(updateQuery);
        
        
        
        System.out.println("Raw KB ("+i+" concepts || "+nbObjProp+" objProps) created!");
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
    
    
    
    public void exportKBToFile(String fileName)
    {
        this.spOut.writeKBFile(fileName);
    }
    
}
