T2RKB
====

Transform a skos thesaurus to a raw knowledge base (use Fuseki Sparql Endpoint as input and output)
* Each skos:concept is transformed into owl:Class
* Each skos:broader is transformed into rdfs:subClassOf
* subClassOf properties verification using WordNet 3.0
* Each RT is generalised by a global objectProperty and specialised with sub properties defined by existencial restrictions instead of domain/range
* All labels (prefered and alternative) are preserved thanks rdfs:label with their corresponding lang etiquette
