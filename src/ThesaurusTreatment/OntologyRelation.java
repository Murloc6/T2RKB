/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ThesaurusTreatment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import net.sf.json.JSONObject;
import t2kb.SparqlProxy;

/**
 *
 * @author murloc
 */
public class OntologyRelation 
{
    private String relURI;
    
    private String label;
    
     private ArrayList<String> domains = new ArrayList<>();
    private ArrayList<String> ranges = new ArrayList<>();
   
    private int idSubProperties;
    
    private HashMap<String, String> subPropertiesDomainRange;
    
    private SparqlProxy spOut;
    
    public OntologyRelation(String uri, SparqlProxy spOut)
    {
        this.relURI = uri;
        this.spOut = spOut;
        //this.subPropertiesDomainRange = new HashMap<>();
        this.idSubProperties = 1;
        this.label = "";
        String sUp = this.relURI.substring(this.relURI.lastIndexOf("#")+1);
        String[] sTab = sUp.split("(?=\\p{Lu})");
        for(String s : sTab)
        {
            this.label += s.toLowerCase()+" ";
        }
    }
    
    
    /*public boolean isTopClass(ArrayList<String> list, String uri)
    {
        boolean ret = true;
        boolean stop = false;
        if(!list.contains(uri))
        {
            Iterator<String> it = list.iterator();
            while(!stop && it.hasNext())
            {
                String d = it.next();
                if(this.spOut.isSubClassOfStar(uri, d))
                {
                    ret = false;
                    stop = true;
                }
                else if(this.spOut.isSubClassOfStar(d, uri))
                {
                    stop = true;
                    list.remove(d);
                }
            }
        }
        else
        {
            ret = false;
        }
        return ret;
        
    }*/
    
    private ArrayList<String> getTop(String type)
    {
        ArrayList<String> ret = new ArrayList<>();
        String query = "SELECT DISTINCT ?dom WHERE { "+
                                      "?rel rdfs:subPropertyOf <"+this.relURI+">. "+
                                      "?rel "+type+" ?dom. "+
                                      "FILTER NOT EXISTS{"+
                                            "?rel2 rdfs:subPropertyOf <"+this.relURI+">. "+
                                            "?rel2 "+type+" ?dom2. "+
                                            "?dom rdfs:subClassOf+ ?dom2."+
                                        "}"+
                                    "}";
        ArrayList<JSONObject> rep = this.spOut.sendQuery(query);
        for(JSONObject jsono : rep)
        {
            ret.add(jsono.getJSONObject("dom").getString("value"));
        }
        return ret;
    }
    
    public ArrayList<String> getTopDomain()
    {
        return this.getTop("rdfs:domain");
    }
    
    public ArrayList<String> getTopRange()
    {
        return this.getTop("rdfs:range");
    }
    
    public String addRangeDomain(String uriDomain, String uriRange)
    {
        
        /*if(this.isTopClass(this.domains, uriDomain))
        {
            this.domains.add(uriDomain);
        }
        if(this.isTopClass(this.ranges, uriRange))
        {
            this.ranges.add(uriRange);
        }*/
        
        
        String subPropUri = this.relURI+"_"+this.idSubProperties;
        String ret = "<"+subPropUri+"> rdf:type owl:ObjectProperty; rdfs:subPropertyOf <"+this.relURI+">; rdfs:label \""+this.label+"\".";
        //rdfs:domain <"+uriDomain+">; rdfs:range <"+uriRange+">.
        
        ret += "<"+uriDomain+">  rdfs:subClassOf  [ a owl:Restriction ; " +
        " owl:onProperty <"+subPropUri+"> ; " +
        " owl:allValuesFrom <"+uriRange+">" +
        " ] .";
        
        this.idSubProperties ++;
        
        return ret;
        //this.subPropertiesDomainRange.put(uriDomain, uriRange);
    }
    
    
    
    
    /*private ArrayList<String> getUppestSparql(ArrayList<String> initList, SparqlProxy spOut)
    {
        ArrayList<String> ret = new ArrayList<>();
        
        //String uriList = "";
        StringBuilder tempQuery = new StringBuilder("INSERT DATA {");
        for(String s : initList)
        {
            //uriList += "ag:"+s.substring(s.lastIndexOf("/")+1)+" ";
            tempQuery.append("<http://T2KB.com#test> <http://T2KB.com#isInList> <"+s+">.");
        }
        tempQuery.append("}");
        spOut.storeData(tempQuery);
        
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
                                    "PREFIX ag: <http://aims.fao.org/aos/agrovoc/>"+
                                    "SELECT DISTINCT ?x1 WHERE" +
                                    "{" +
                                    "<http://T2KB.com#test> <http://T2KB.com#isInList> ?x1." +
                                    "MINUS { SELECT ?x1 WHERE {\n" +
                                                    "<http://T2KB.com#test> <http://T2KB.com#isInList> ?x1." +
                                                    "<http://T2KB.com#test> <http://T2KB.com#isInList> ?x2." +
                                                    "	?x1 rdfs:subClassOf* ?x2." +
                                                    "FILTER (?x1 != ?x2)" +
                                            " }"+
                                    "}" +
                                    "}";
        
        ArrayList<JSONObject> rep = this.spOut.sendQuery(query);
        for(JSONObject jsono : rep)
        {
            ret.add(jsono.getJSONObject("x1").getString("value"));
        }
        tempQuery = new StringBuilder("DELETE WHERE { <http://T2KB.com#test> <http://T2KB.com#isInList>  ?c.}");
        spOut.storeData(tempQuery);
        return ret;
    }*/
    
    public String toTtl(SparqlProxy spOut)
    {
        String ret = "<"+this.relURI+"> rdf:type owl:ObjectProperty.";
        /*ret += "rdfs:domain [ a owl:Class; owl:unionOf (";
        for(String domain : this.getTopDomain())
        {
            ret += " <"+domain+"> ";
        }
        ret += ")];";*/
        
        /*ret += "rdfs:range [ a owl:Class; owl:unionOf (";
        for(String range : this.getTopRange())
        {
            ret += " <"+range+"> ";
        }
        ret += ")].";*/
        
        
        return ret;
    }
    
    /*public StringBuilder getSubPropertiesTtl()
    {
        StringBuilder ret = new StringBuilder("");
        
        int id = 1;
        for(Entry<String, String> entry : this.subPropertiesDomainRange.entrySet())
        {
            ret.append("<"+this.relURI+"_"+id+"> rdf:type owl:ObjectProperty; rdfs:subPropertyOf <"+this.relURI+">; rdfs:domain <"+entry.getKey()+">; rdfs:range <"+entry.getValue()+">; rdfs:label \""+this.label+"\".");
            id ++;
        }
                
        return ret;
    }*/
    
}
