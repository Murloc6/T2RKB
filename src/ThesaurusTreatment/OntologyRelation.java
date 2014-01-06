/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ThesaurusTreatment;

import java.util.ArrayList;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import t2kb.SparqlProxy;

/**
 *
 * @author murloc
 */
public class OntologyRelation 
{
    private String relURI;
    
    private ArrayList<String> ranges = new ArrayList<>();
    private ArrayList<String> domains = new ArrayList<>();
    
    private SparqlProxy spOut;
    
    public OntologyRelation(String uri, SparqlProxy spOut)
    {
        this.relURI = uri;
        this.spOut = spOut;
    }
    
    public void addRange(String uriRange)
    {
        if(!this.ranges.contains(uriRange))
        {
            this.ranges.add(uriRange);
        }
    }
    
    public void addDomain(String uriDomain)
    {
        if(!this.domains.contains(uriDomain))
        {
            this.domains.add(uriDomain);
        }
    }
    
    
    
    private ArrayList<String> getUppestSparql(ArrayList<String> initList)
    {
        ArrayList<String> ret = new ArrayList<>();
        
        String uriList = "";
        for(String s : initList)
        {
            uriList += " <"+s+"> ";
        }
        
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +
                                    "SELECT DISTINCT ?x1 WHERE" +
                                    "{" +
                                    "VALUES ?x1 {"+uriList+"}" +
                                    "MINUS { SELECT ?x1 WHERE {\n" +
                                                    "VALUES ?x1 {"+uriList+"}" +
                                                    "VALUES ?x2 {"+uriList+"}" +
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
        return ret;
    }
    
    public String toTtl()
    {
        String ret = "<"+this.relURI+"> rdf:type owl:ObjectProperty;";
        for(String domain : this.getUppestSparql(this.domains))
        {
            ret += " rdfs:domain <"+domain+">;";
        }
        for(String range : this.getUppestSparql(this.ranges))
        {
            ret += " rdfs:range <"+range+">;";
        }
        ret = ret.substring(0, ret.lastIndexOf(";"));
        ret += ".";
        
        return ret;
    }
    
}
