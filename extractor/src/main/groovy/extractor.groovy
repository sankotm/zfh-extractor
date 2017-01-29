/**
 * Extractor dat ze ZFH I, ZFH II
 * @author Michal Sankot on 29. 1. 2017.
 */

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.cyberneko.html.parsers.SAXParser

String normalize(String text) {
  return text.replaceAll('\n', ' ').replaceAll('\\u00A0', '').replaceAll('\\s+', ' ')
}

String printXml(Node node) { 
  StringWriter stringWriter = new StringWriter()
  XmlNodePrinter nodePrinter = new XmlNodePrinter(new IndentPrinter(stringWriter))
  nodePrinter.print(node)
  return stringWriter.toString()
}

String zdroj = null
Node html = new XmlParser(new SAXParser()).parse('src/main/resources/zfh1.htm'); zdroj = "Zlatý fond her I"
//Node html = new XmlParser(new SAXParser()).parse('src/main/resources/zfh2.htm'); zdroj = "Zlatý fond her II"

List hry = []

Map hra = [:]
String attr = null

Map atributyTabulky = [
    "Fyzická zátěž"     : "fyzicka_zatez",
    "Psychická zátěž"   : "psychicka_zatez",
    "Čas na přípravu"   : "cas_na_pripravu",
    "Instr. na přípravu": "instruktoru_na_pripravu",
    "Čas na hru"        : "cas_na_hru",
    "Instr. na hru"     : "instruktoru_na_hru",
    "Počet hráčů"       : "pocet_hracu",
    "Věková kategorie"  : "vekova_kategorie",
    "Prostředí"         : "prostredi",
    "Denní doba"        : "denni_doba",
    "Roční období"      : "rocni_obdobi",
    "Roční doba"        : "rocni_obdobi",
    "Materiál"          : "material"
]

Map atributyFontu = [
    "Cíl"               : "cil",
    "Charakteristika"   : "charakteristika",
    "Libreto"           : "libreto",
    "Realizace hry"     : "realizace",
    "Metodické poznámky": "metodicke_poznamky",
    "Metodická poznámka": "metodicke_poznamky"
]

def addText = { ihra, iattr, text ->
  String normalizedText = normalize(text)

  if (ihra[iattr]) {
    ihra[iattr] = ihra[iattr] + "\n" + normalizedText
  } else {
    ihra[iattr] = normalizedText
  }
}

elementy = [:]

html.'**'.each { it ->


  if (it.getClass().simpleName == "String") {
//    println it
    if (attr != null) {
      addText(hra, attr, it)
    }

  } else {
    Node node = (Node) it
    String nodeName = node.name()

    elementy[nodeName] = nodeName

    if (nodeName == "B") {
      addText(hra, attr, "<b>" + node.text() + "</b>")
    } else if (nodeName == "I") {
      addText(hra, attr, "<i>" + node.text() + "</i>")
    } else if (nodeName == "IMG") {
      addText(hra, attr, "<img>" + node.@src + "</img>")
    } else if (nodeName == "FONT") {
      if (node.@color == "green" && node.children()[0].@size == '+3') {
        if (!hra.isEmpty()) {
          hry.add(hra)
        }

        hra = [zdroj:zdroj]
        hra.nazev = node.children()[0].text().replace('\n', '').toLowerCase().capitalize().replaceAll('\\s+', ' ')
        println "----------------------------------------- ${hra.nazev} ---------------------------------------"
        attr = 'autor' // nekdy byva po nazvu hry uvedeny autor


      }
      if (node.@color == "red" && node.children()[0].@size == '+1') {

        String nazevAtributu = normalize(node.children()[0].text())

        String atributHry = atributyFontu[nazevAtributu]
        if (atributHry) {
          attr = atributHry
        } else {
          println " =======================================> Ad-hoc sekce: " + nazevAtributu
          attr = 'ad_hoc'

          hra[attr] = (hra[attr] ? hra[attr] + "\n" : '') + "<section-heading>$nazevAtributu</section-heading>"
        }
      }
    } else if (nodeName == "TABLE") {

      // tabulka atributu hry
      if (node.@bgcolor == "lightgreen") {
        attr = null

        node.children()[0].each { Node tableNode ->
          if (tableNode.name() == "TR") {
            String nazevAtributu = normalize(tableNode.children()[0].text())
            String hodnota = normalize(tableNode.children()[1].text())

            String atributHry = atributyTabulky[nazevAtributu]
            if (atributHry) {
              hra[atributHry] = hodnota
            } else {
              println ">>>>>>>>>>>>>>>>>>>>>>>>>>>>> Neznamy atribut tabulky: $nazevAtributu"
            }
          }
        }

      } else {
        // tabulka v textu - nechame ji jak je
        addText(hra, attr, printXml(node))
      }
    }
  }
}

hry.add(hra)

Gson gson = new GsonBuilder().setPrettyPrinting().create()
println gson.toJson(hry)

println elementy.keySet()