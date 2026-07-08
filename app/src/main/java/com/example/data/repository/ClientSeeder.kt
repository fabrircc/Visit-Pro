package com.example.data.repository

import com.example.data.model.Client

object ClientSeeder {
    fun parseClients(): List<Client> {
        val list = mutableListOf<Client>()
        val lines = RAW_DATA.trim().split("\n")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank() || trimmedLine.startsWith("﻿;SEQ.;NOME") || trimmedLine.startsWith(";SEQ.;")) continue
            val parts = trimmedLine.split(";")
            if (parts.size < 5) continue
            
            // parts[0] or parts[1] might hold SEQ
            val seqVal = parts.getOrNull(0)?.trim()?.ifEmpty { parts.getOrNull(1)?.trim() } ?: ""
            // Name is at index 2 (usually after the double semi-colon or sequence)
            val nameVal = parts.getOrNull(2)?.trim() ?: ""
            if (nameVal.isEmpty()) continue
            
            val endVal = parts.getOrNull(3)?.trim() ?: ""
            val bairroVal = parts.getOrNull(4)?.trim() ?: ""
            val contatoVal = parts.getOrNull(5)?.trim() ?: ""
            val telefoneVal = parts.getOrNull(6)?.trim() ?: ""
            val pgtoVal = parts.getOrNull(7)?.trim() ?: ""
            
            // Categorize Segment based on client name
            val segmentVal = when {
                nameVal.contains("Mercadinho", ignoreCase = true) || 
                nameVal.contains("Mercearia", ignoreCase = true) || 
                nameVal.contains("Supermercado", ignoreCase = true) || 
                nameVal.contains("Supermecado", ignoreCase = true) || 
                nameVal.contains("A & R Super", ignoreCase = true) || 
                nameVal.contains("Peg Pag", ignoreCase = true) ||
                nameVal.contains("Comercial", ignoreCase = true) -> "Supermercado / Mercearia"
                
                nameVal.contains("Açougue", ignoreCase = true) || 
                nameVal.contains("Carne", ignoreCase = true) || 
                nameVal.contains("Carnes", ignoreCase = true) -> "Açougue"
                
                nameVal.contains("Padaria", ignoreCase = true) || 
                nameVal.contains("Panificadora", ignoreCase = true) || 
                nameVal.contains("Pão", ignoreCase = true) || 
                nameVal.contains("Pães", ignoreCase = true) || 
                nameVal.contains("Padoca", ignoreCase = true) ||
                nameVal.contains("Panificação", ignoreCase = true) ||
                nameVal.contains("Pao", ignoreCase = true) -> "Padaria / Panificadora"
                
                nameVal.contains("Boutique", ignoreCase = true) || 
                nameVal.contains("Emporio", ignoreCase = true) || 
                nameVal.contains("Empório", ignoreCase = true) || 
                nameVal.contains("Qualita", ignoreCase = true) ||
                nameVal.contains("Conveniência", ignoreCase = true) ||
                nameVal.contains("Conveniência ", ignoreCase = true) ||
                nameVal.contains("Loja de Conveniencia", ignoreCase = true) -> "Empório / Boutique"
                
                nameVal.contains("Lanchonete", ignoreCase = true) || 
                nameVal.contains("Burger", ignoreCase = true) || 
                nameVal.contains("Hotburguer", ignoreCase = true) || 
                nameVal.contains("Fast Food", ignoreCase = true) || 
                nameVal.contains("Restaurante", ignoreCase = true) || 
                nameVal.contains("Pizzaria", ignoreCase = true) ||
                nameVal.contains("Café", ignoreCase = true) ||
                nameVal.contains("Tenda Do", ignoreCase = true) -> "Alimentação / Lanchonete"
                
                else -> "Geral"
            }
            
            // Deterministic Lat/Lng coordinates around Uberlândia, MG (-18.9186, -48.2772)
            // Use String hash to generate deterministic, realistic looking lat/lng points
            val hashCode = nameVal.hashCode().toDouble()
            val latOffset = (hashCode % 100) / 1500.0 // +/- 0.06 deg (~6km grid)
            val lngOffset = ((hashCode / 100).toInt() % 100) / 1500.0
            val baseLat = -18.9186 + latOffset
            val baseLng = -48.2772 + lngOffset
            
            list.add(
                Client(
                    seq = seqVal,
                    name = nameVal,
                    address = endVal,
                    neighborhood = bairroVal,
                    contact = contatoVal,
                    phone = telefoneVal,
                    paymentTerm = pgtoVal,
                    segment = segmentVal,
                    latitude = baseLat,
                    longitude = baseLng
                )
            )
        }
        return list
    }
    
    private val RAW_DATA = """
;;Adgmar Alves Da Silva ;Av Floriano Peixoto n 5279 ;Alto Umuarama;Adgmar;99968-8486;Boleto 10;;;;;;;;;;;;;;;;;;
1597;;Afonso Eder Ribeiro ;Av Floriano Peixoto n 5244;Alto Umuarama;Nilda ;99764-0018;Boleto 10;;;;;;;;;;;;;;;;;;
1675;;Mercadinho e Açougue Carajas Ltda ;Rua professor Eudoxio Casasanta Pereira 460;Carajas ;;;;;;;;;;;;;;;;;;;;;
1662;;LB Investimento Financeiro ;R. Duque De Caxias n 450;Centro;Salete;99290-1500;Boleto 10;;;;;;;;;;;;;;;;;;
;;Adoro Boutique De Carne E Trein;Rua Das Papoulas n 878;Cidade Jardim;Carol;99982-9832;Boleto 10;;;;;;;;;;;;;;;;;;
1647;;Há + Hortifruiti & Emporio Jardins;Av Uirapuru n 1260;Cidade Jardim;Janaina;99892-6442;Boleto 14;;;;;;;;;;;;;;;;;;
1714;;Sacolao sempre Verde ;Rua dos Eucaliptos 434;Cidade Jardim;Cidade Jardin;;;;;;;;;;;;;;;;;;;;
;;Anesio Antonio De Assunçao;Rua Paulo De Frontin n 803;Custodio Pereira ;Anesio;99148-1410;Boleto 10;;;;;;;;;;;;;;;;;;
1626;;Ednaldo Henrique Da Silva ;Rua Paulo De Frontim n 1051;Custodio Pereira ;Ednaldo;98800-8712;Boleto 10;;;;;;;;;;;;;;;;;;
;;Ednamar Aparecida da Silva Souto ;R Piaui n 2718;Custodio Pereira ;Ednamar;99632-5573;Boleto 10;;;;;;;;;;;;;;;;;;
;;Martins E Silva Padaria Ltda ;Rua Paulo De Frontin n 785;Custodio Pereira ;Cybeli;98897-5518;Boleto 14;;;;;;;;;;;;;;;;;;
;;Sacolão E Emporio Perola ;Av Floriano Peixoto n 5214;Custodio Pereira ;Eurania ;98856-0214;Boleto 10;;;;;;;;;;;;;;;;;;
1848;;Comercial Pavone Ltda;Av Solidariedade n 1040;Dom Almir;valdeir;998630764;Boleto 10;;;;;;;;;;;;;;;;;;
;;RS Gomes Ltda;Av Solidariedade n 1310;Dom Almir;Marquinho/Fabiola;99694-7811;Boleto 10;;;;;;;;;;;;;;;;;;
;;E Lopes Nobre Souza ;Av Solidariedade n 435;Dom Almir ;Elizangela;99210-4956;Boleto 10;;;;;;;;;;;;;;;;;;
;;RR Distr De Produto Alimentício E Limpeza ;Av Solidariedade n 460;Dom Almir ;Roni/Andre;99690-0010;Boleto 10;;;;;;;;;;;;;;;;;;
1664;;LLN Comércio De Alimentos Ltda;Av Paulo Gracinto n 15 Lj 87;Gavea;;98826-1210;Boleto 10;;;;;;;;;;;;;;;;;;
1687;;Padoca sul;Rua da Carioca 2077;Gavea;;;;;;;;;;;;;;;;;;;;;
1684;;MontreL Express LTDA;Av Manuel Lucio n 591;Gran ville;Marcelo;98405-8061;Boleto 10;;;;;;;;;;;;;;;;;;
1640;;Emporio Sales ;Av Itaipu n 123;Granada ;Paulo ;99275-0606;10 Dias ;;;;;;;;;;;;;;;;;;
;;Hotburguer Santos E Resende;Av Itaipu n 717;Granada ;;99109-6619;;;;;;;;;;;;;;;;;;;
1937;;Comercial Salto Grande Ltda;Av Alipio Abraao n 943;Gravatas ;Jhieni;99102-6874;14 Dias;;;;;;;;;;;;;;;;;;
1623;;Domingues Lima Comercio Eirelli ;Al Bertoldo Antonio Borges n 290;Jadim Das Palmeiras II;Plierrio;99992-3219;Boleto 10;;;;;;;;;;;;;;;;;;
;;Panificadora Pingo Doce Ltda;R Alemanha n 595;Jardim Botanico;Marinilva;99150-5611;10 Dias ;;;;;;;;;;;;;;;;;;
1791;;Qualita Emporio Mendonça ;Av Dos Ferreiras n 565;Jardim California ;Ronieli ;98859-1980;Boleto 7;;;;;;;;;;;;;;;;;;
1595;;A & R Supermecados ;Av Jerusalem n 1066;Jardim Canãa;Paulo;99951-7939;Boleto 10;;;;;;;;;;;;;;;;;;
;;Casa De Carnes E Mercearia Pingo Doce ;AV Juda n 311;Jardim Canãa;Willian;99682-9501;Boleto 10;;;;;;;;;;;;;;;;;;
1594;;Leandro Gomes Andrade Almeida ;Av Babel n 452;Jardim Canãa;Leandro ;99640-5062;Boleto 10;;;;;;;;;;;;;;;;;;
1663;;Leandro Silva Mamede E Filha Supermercado ;Av Juda n 344;Jardim Canãa;Leandro ;99687-6787;Boleto 10;;;;;;;;;;;;;;;;;;
;;Edimar Batista Pinto Emporio ;R Da Flor De Cera n 30;Jardim Celia ;Batista ;99233-3670;Boleto 10;;;;;;;;;;;;;;;;;;
;;Marília Gabrielle Viana Freitas ;R Das Paineiras n 94;Jardim Celia ;Marcos;99323-9573;Boleto 10;;;;;;;;;;;;;;;;;;
1701;;Pitter Marcos Pereira ;Av Dos Lirios Amarelos n 715;Jardim Celia ;Pitter;3255-9702;Boleto 10;;;;;;;;;;;;;;;;;;
;;Comercial Triangulo Ltda;R Dos Tico-Tico n 360;Jardim Das Palmeiras;Leandro ;99166-5903;Boleto 10;;;;;;;;;;;;;;;;;;
1625;;E.A.J. Junior Emporio Ltda;Al Bladilei Alves Cardoso n 455;Jardim Das Palmeiras II;Junior;99243-6556;Boleto 10;;;;;;;;;;;;;;;;;;
1621;;D Gust Paes E Rotisseria LTDA ME;Av Izau Rangel ;Jardim Finotti;Juliano;99975-2888;Boleto 14;;;;;;;;;;;;;;;;;;
1702;;PNG Panificadora Ltda;Av Joao Naves De Avila n 3340;Jardim Finotti;Pedro ;98876-3429;Boleto 7;;;;;;;;;;;;;;;;;;
1842;;Pao Do Dia Foods Ltda;Al Jardim Holanda n 851;Jardim Holanda;Aline;99911-3999;Boleto 10;;;;;;;;;;;;;;;;;;
1703;;Qualita Emporio Martins Ltda;Al José De Oliveira Guimarães n 718 lote 26B;Jardim Holanda;Ronieli;98859-1980;Boleto 10;;;;;;;;;;;;;;;;;;
1634;;Emporio da cerveja e Mercearia ;Al Jose De Oliveira Guimarães n 869;Jardim Holanda ;Dayane;99977-7291;Boleto 10;;;;;;;;;;;;;;;;;;
;;Emporio Da Hora ;Al Jardim Holanda n 343;Jardim Holanda ;Adailton;99792-6152;Boleto 10;;;;;;;;;;;;;;;;;;
1330;;Emporio Holanda Ltda ;Av Alice De Oliveira E Silva n 85;Jardim Holanda ;Aline;99911-3999;Boleto 10;;;;;;;;;;;;;;;;;;
1693;;Panificadora E Empório Udi ;Al Jose De Oliveira Guimarães n 629;Jardim Holanda ;Mauro;99353-1000;Boleto 10;;;;;;;;;;;;;;;;;;
;;Simone De Oliveira Alcantara Gomes;Av Sideral n 878;Jardim Ipanema;Simone/Sandra;99233-3754;Boleto 10;;;;;;;;;;;;;;;;;;
;;Supermercado Thiago Oliveira Ltda;Av Sideral n 1320;Jardim Ipanema;Thiago/Maceio;99190-8007;Boleto 10;;;;;;;;;;;;;;;;;;
1976;;B&J Empreendimentos Alimenticios ;Praca Jose Alves dos Santos 100;Jardim Ipanema ;;;;;;;;;;;;;;;;;;;;;
;;Conveniencia E Panificadora Karaiba Eireli;R Rafael Marino Neto n 600;Jardim Karaiba;Ricardo;99116-2041;10 Dias;;;;;;;;;;;;;;;;;;
1831;;MA e Me Comercio de Frutas e Verduras ;R Rafael Marino Neto n 563;Jardim Karaiba;Tercilo/Cintia;99334-0848;10 Dias;;;;;;;;;;;;;;;;;;
;;Filhos De Minas Supermercado Ltda ;Rua Au-3 n 240;Jardim Karaiba ;Marina;99977-2078;Boleto 10;;;;;;;;;;;;;;;;;;
1763;;Pamonharia Patos de Milho;Av Alipio Abraao 1987;Jardin Botanico;;;;;;;;;;;;;;;;;;;;;
1948;;Vinicius Mendes dos Santos Vig ;Av Jerusalen 459;Jardin Canaa;;;;;;;;;;;;;;;;;;;;;
1949;;Pizzaria e Hamburgueria Canaa;Av Jerusalen 164;Jardin canaa;;;;;;;;;;;;;;;;;;;;;
1595;;Marcela Magda Braga ;Al Jardin Holanda 343;Jardin Holanda;;;;;;;;;;;;;;;;;;;;;
1959;;Jgc Supermecado Ltda (super. Industrial);Alameda Jardin Holanda 471;Jardin Holanda;;;;;;;;;;;;;;;;;;;;;
1679;;Mercearia e Alimentos Bela Vista ;Av Landscape 2330;Jardin Sul;;;;;;;;;;;;;;;;;;;;;
1717;;São Lucas Panificadora Ltda;R Do Estivador n 624;Jd Das Palmeiras;Adriana;99663-3143;Boleto 10;;;;;;;;;;;;;;;;;;
;;Família Moraes Alimentos Ltda;Av Paulo Firmino n 1060;Jd Das Palmeiras ;Weliton;9293-4900;Boleto 10;;;;;;;;;;;;;;;;;;
1637;;Emporio Líder Ltda;R Planalto Da Borborena n 1.700;Laranjeira;Douglas;99676-7917;10 Dias;;;;;;;;;;;;;;;;;;
;;Emporio Marselha;R Marselha n 115;Laranjeiras;Ana Carolina;99111-8418;10 Dias;;;;;;;;;;;;;;;;;;
1657;;Emporio Sales ;Av Iraque 209;Laranjeiras;;;;;;;;;;;;;;;;;;;;;
1886;;SUPERMERCADO LARANJEIRAS LTDA-EPP;AVENIDA CONTINENTAL,383;Laranjeiras;Cristiano;99177-1709;Boleto 10.;;;;;;;;;;;;;;;;;;
1772;;Classic Supermercado;Av Continental n 648;Laranjeiras ;Jose;99121-9759;10 Dias;;;;;;;;;;;;;;;;;;
1615;;Comercial Fabio Maia Ltda;Rua Royalites n 682;Laranjeiras ;Fabio;99123-9743;10 Dias;;;;;;;;;;;;;;;;;;
;;Emporio Botanico Eirelli Eireli;Av Iraque n 590;Laranjeiras ;Manoel;99652-3571;10 Dias;;;;;;;;;;;;;;;;;;
1657;;Julio Cesar De Sales ;Av Iraque n 209;Laranjeiras ;Julio;3219-9665;10 Dias;;;;;;;;;;;;;;;;;;
;;MG Supermercado Eirelli ;Av Serra Bodoquera n 638;Laranjeiras ;Rosa ;99243-9943;10 Dias;;;;;;;;;;;;;;;;;;
1766;;Sandra Pereira Passos-Me;Rua Dos Titos n 713;Laranjeiras ;Sandra ;99861-2510;10 Dias;;;;;;;;;;;;;;;;;;
1728;;Supermercado Negreiro Ltda ;Av Iraque n 551;Laranjeiras ;Anderson;3225-4448;10 Dias;;;;;;;;;;;;;;;;;;
;;Uellen Soares De Sousa ;R Constituição n 263;Laranjeiras ;Wilson;99183-6020;10 Dias;;;;;;;;;;;;;;;;;;
;;Minas Goias Comercio De Alimento ;Av Rondon Pacheco n 3671;Lidice;;(64)99977-9181;Boleto 10;;;;;;;;;;;;;;;;;;
;;Minas Goias Comercio De Alimento ;Av Rondon Pacheco n 3671;Lidice;;(64)99977-9181;Boleto 10;;;;;;;;;;;;;;;;;;
;;Maria Elidivania Lopes Nobre ;R Odilon Costa Azevedo n 397;Lot Integração ;Elidivania;99180-6656;Boleto 10;;;;;;;;;;;;;;;;;;
1673;;Martins Supermecado ;Rua Rodrigues da Cunha 763;Martins;;;;;;;;;;;;;;;;;;;;;
;;Silva Soares Alimentos Ltda ;Av Doris Greco Candeloro n 250;Monte Hebron;Ivone ;99974-1584;Boleto 10;;;;;;;;;;;;;;;;;;
;;Supermercado Matheus Venancio;Av Contador Jose Candeloro n 834;Monte Hebron;Matheus;99230-5946;Boleto 10;;;;;;;;;;;;;;;;;;
1644;;FGH Alimentos ;Av Francisco Galassi 839;Morada Colina;;;;;;;;;;;;;;;;;;;;;
1686;;Nosso Emporio Carioca;Rua Da Carioca n 1507;Morada Da Colina;Relga;99632-0819;Boleto 10;;;;;;;;;;;;;;;;;;
1368;;LMBL Café LTDA ME;Av Paulo Gracindo n 15;Morada Da Colina ;Tatiana;99663-4286;Boleto 10;;;;;;;;;;;;;;;;;;
1823;;Patio Amareto comercio de Alimentos ;Rua dos Vinhedos 50;Morada da Colina ;;;;;;;;;;;;;;;;;;;;;
1869;;Leandro da silva santos ;av Aldo Borges Leao 2139;Morada Nova ;;;;;;;;;;;;;;;;;;;;;
1732;;THL Supermercado ;Av Aldo Borges Leão n 1847;Morada Nova ;Lucas;98431-5263;Boleto 10;;;;;;;;;;;;;;;;;;
1610;;Casa De Carnes Melo E Souza Ltda;R Serraria n 15;Morumbi;Sergio;99199-6362;Boleto 10;;;;;;;;;;;;;;;;;;
;;Joao Olinto Pereira ;Av Alexi Abrahão n 591;Nova Uberlandia ;Joao;99830-0333;Boleto 10;;;;;;;;;;;;;;;;;;
1670;;Manoel A R Junior Panificadora LTDA;R Dos Pica-Paus n 915;Nova Uberlandia ;Junior;99889-0350;Boleto 10;;;;;;;;;;;;;;;;;;
1630;;Emporio Andrade Gourmet ltda;Av Ubirajara Zacarias n 737;Novo Mundo ;Fernands;99631-4485;Boleto 10;;;;;;;;;;;;;;;;;;
1652;;Iva Alves Da Silva ;R Samanea n 106;Panorama;Iva ;99869-1955;Boleto 10;;;;;;;;;;;;;;;;;;
1899;;Gastronomia Ac Ltda (olipop) ;Avenida liberdade 234 ;Patrimonio ;;;;;;;;;;;;;;;;;;;;;
1342;;Via Sul Conveniencia E Alimentos LTDA ME;Rua Das Cariocas 2077;Patrimonio ;Werley;99161-2794;Boleto 10;;;;;;;;;;;;;;;;;;
;;Elio Felipe De Oliveira Gomes ;Av Wilson Rodrigues Da Silva n 1580;Pequis ;Felipe;99300-0000;Boleto 10;;;;;;;;;;;;;;;;;;
1708;;Rodrigues E Carvalho Alimentos Ltda;Av Wilson Rodrigues Da Silva n 1791;Pequis ;Ivone ;99974-1584;Boleto 10;;;;;;;;;;;;;;;;;;
1715;;Santa Clara Panificadora ;Av Wilson Rodrigues Da Silva n 641;Pequis ;Antonio;99148-9636;Boleto 10;;;;;;;;;;;;;;;;;;
;;Emporio Lidice LTDA;Rua Rio Preto n 202;Povoa;Wesley;(31) 99371-2999;Boleto 10;;;;;;;;;;;;;;;;;;
;;Romes Mendes Costa;AV Aldo Leao Borges Leão n 2543;Residencial Lago Azul;Vanessa;99968-4160;Boleto 10;;;;;;;;;;;;;;;;;;
;;Salgados Didu Ind E Com De Produtos Alim.;R Altivo Ferreira Batista n 594;Residencial Viviane;Mariana;99686-4588;Boleto 10;;;;;;;;;;;;;;;;;;
1312;;Comercial Monte Alto;Av Alipio Abraao n 995;Santa Luzia;Jhieni;99102-6874;14 Dias;;;;;;;;;;;;;;;;;;
1667;;Loja de Conveniencia da Saida ;Av joao Naves de Avila 7066;Santa Luzia;;;;;;;;;;;;;;;;;;;;;
;;Gabriel De Melo E Silva Carneiro ;Av Jose Rezende Costa n 453;Santa Maria;Gabriel;99855-4530;Boleto 10;;;;;;;;;;;;;;;;;;
;;Gabriel De Melo E Silva Carneiro ;Av Jose Rezende Costa n 453;Santa Maria;Gabriel;99855-4530;Boleto 10;;;;;;;;;;;;;;;;;;
1697;;Panificadora Santo Agostinho LTDA;Rua Manoel dos Santos n 163;Santa Maria;Agostinho;98825-1642;Boleto 14;;;;;;;;;;;;;;;;;;
1611;;Casa de carnes santos e silva ;Av Alexandre Ribeiro Guimaraes  330;Santa maria;;;;;;;;;;;;;;;;;;;;;
1604;;Bella Vision Rest e Ki Biscoito;Av Segismundo Pereira 2236;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1607;;Bruno Mendes De Souza Falbo;Av Cesar Finotti n 500 2;Santa Monica;Carol;99680-9510;Boleto 10;;;;;;;;;;;;;;;;;;
1630;;Central Delivery Comércio De Alimento;Av Segismundo Pereira n 1824;Santa Monica;Cristiano;99995-1978;Boleto 10;;;;;;;;;;;;;;;;;;
1790;;LA Alimentos Ltda;Rua Izau Rangel de Mendonca 907;Santa Monica;Adauto;;;;;;;;;;;;;;;;;;;;
1658;;LC Paulinho Comercio;Av Doutor Laerte Vieira Gonçalves 1330;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1885;;lol Comércio De Alimentos Ltda;Av João Naves de Ávila n 2121 bl 3q box 4;Santa Monica;Jose;99119-8239;Boleto 28;;;;;;;;;;;;;;;;;;
1689;;Panificadora Alves e França ;Av Ana Godoy de souza 1869;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1593;;Randerson Carlos Nicomedes;Av Salomão Abrahao 393;Santa Monica;randerson;99886-2610;;;;;;;;;;;;;;;;;;;
1767;;Saulo Nunes de Freitas ;Av segismundo pereira 2056;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1722;;Supermecado e Armazen Primo;Av Salomão Abrahao 1049;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1731;;Thaymar Supermecado ;Av Ana Godoy de souza 2895;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1671;;Marccelo Freitas Guedes Limitada ;Av Salomao Abraao 554;Santa Monica;;;;;;;;;;;;;;;;;;;;;
1603;;B F Panificação;Av Ana godoy de souza 1010;Santa Monica ;;;;;;;;;;;;;;;;;;;;;
1609;;Casa De Carnes Gomes E Matos Ltda;Av Joao Naves De Avila n 3500;Santa Monica ;Neto;3211-9929;Boleto 10;;;;;;;;;;;;;;;;;;
;;Comercio Real De Carnes LTDA;Av Belarmino Cotta Pacheco n 1040;Santa Monica ;Durval;99103-9001;Boleto 10;;;;;;;;;;;;;;;;;;
1636;;Emporio Guimaraes Uberlandia LTDA;Rua Izau Rangel Mendonça 903;Santa Monica ;Adalto;99963-6867;Boleto 10;;;;;;;;;;;;;;;;;;
;;Estação Express Panificação E Alim;Av Doutor Laerte Vieira Gonçalves n 35;Santa Monica ;Mariana;99197-2763;Boleto 10;;;;;;;;;;;;;;;;;;
;;Loja De Conveniencia Santa Monica ;Av Anselmo Alves n 1000;Santa Monica ;Pedro;99971-9621;Boleto 10;;;;;;;;;;;;;;;;;;
1711;;Sabor e Arte Produtos ;Rua Joao Balbino 1365;Santa Monica ;;;;;;;;;;;;;;;;;;;;;
1957;;Panificadora Cardoso e Martins ;Av Doutor Laerte Vieira Gonçalves 1330;Santa Monica ;;;;;;;;;;;;;;;;;;;;;
1961;;Restaurante Segredo de Minas ;Avenida Francisco Ribeiro 315;Santa Monica ;;;;;;;;;;;;;;;;;;;;;
1761;;Mercearia peg pag Lg ltda ;Av Salomao Abraao 1267;Santa Monica ;;;;;;;;;;;;;;;;;;;;;
;;Adailton E Plierrio Comércial Ltda;R Da Enfermeira n 374;Santo Inacio;Adailton;99992-3219;Boleto 10;;;;;;;;;;;;;;;;;;
;;Panificadora & Mercearia Santos Ltda ;R Dos Advogados ;Santo Inacio;Amilton;99694-4048;Boleto 10;;;;;;;;;;;;;;;;;;
;;A Sertaneja Agropet Ltda;R Abelardo Penna n 33;São Jorge;Rosa;99658-2824;10 Dias;;;;;;;;;;;;;;;;;;
1602;;Avenida Dos Paes Ltda ;Av Das Moedas n 475;São Jorge;Matheus ;99889-4512;10 Dias ;;;;;;;;;;;;;;;;;;
1616;;Comercial Moreira e Cunha;R Norival Pereira Alves n 233;São Jorge;Elcio/Leonardo;3211-3011;10 Dias;;;;;;;;;;;;;;;;;;
;;H.A assis ;R Angelo Cunha n 181;São Jorge;Elenice;98806-9629;10 Dias;;;;;;;;;;;;;;;;;;
1649;;Igor Fonseca De Oliveira ;Av Serra Canastra n 620;São Jorge;Igor;98443-2927;10 Dias;;;;;;;;;;;;;;;;;;
;;Jennifer Ketlen Santos Andrade ;R Taxista Fabio Cardoso n 363 ;São Jorge;Samia;3222-1036;10 Dias;;;;;;;;;;;;;;;;;;
;;Juanicy Gomes Vieira;;São Jorge;Juanicy;99953-5383;10 Dias;;;;;;;;;;;;;;;;;;
1683;;Minimercado Ornelas Secos E Molhados ;R Serra Do Tambor n 397;São Jorge;Douglas;99676-7917;10 Dias ;;;;;;;;;;;;;;;;;;
1762;;Ms Supermercado Ltda;R Angelo Cunha n 170;São Jorge;Helena;99887-4492;10 Dias;;;;;;;;;;;;;;;;;;
1690;;Panificadora E Confeitaria Delicias Do Bairro ;Av Seme Simao n 481;São Jorge;Cristiane;3255-9809;10 Dias ;;;;;;;;;;;;;;;;;;
1699;;Pedro Paulo De Santana Me;R Planalto Borborema n 860;São Jorge;Pedro;99691-9875;10 Dias;;;;;;;;;;;;;;;;;;
1774;;Sacolão E Emporio Dos Gemeos Ltda;rua Paulo Maia n 17;São Jorge;Alex;98842-0103;10 Dias;;;;;;;;;;;;;;;;;;
;;Supermercado Lider Novo Eirelli;R Serra Geral n 186;São Jorge;Willian;99880-4826;10 Dias ;;;;;;;;;;;;;;;;;;
1729;;Supermercado TFS Ltda;R Chapada Do Araripe n 157;São Jorge;Talita;99668-1632;10 Dias ;;;;;;;;;;;;;;;;;;
1681;;Supermecado Classic (MG Supermecado);Av Serra Bodoquera n 638;Sao Jorge ;;;;;;;;;;;;;;;;;;;;;
1724;;Supermecado e Emporio Sao Francisco de Assis ;Rua Sao Francisco de Assis 1015;Saraiva;;;;;;;;;;;;;;;;;;;;;
;;Lanchonete Ariana ;Rod BR 050 s/n;Segismundo Pereira ;Ariana;99144-9966;Boleto 10;;;;;;;;;;;;;;;;;;
;;Emporio E Sacolao Brasa ;R Glecio Custodio Spini n 706;Shoping Park;Rodrigo;99178-8572;Boleto 10;;;;;;;;;;;;;;;;;;
;;Erasmo Francisco Ribeiro Ribeiro ;Av Jose Abdumassih n 1357;Shoping Park;Edson;99117-9623;Boleto 10;;;;;;;;;;;;;;;;;;
1340;;Sacolão E Bebidas Torra Torra;Av Jose Abdumassih n 1665;Shoping Park;Simone;99639-6409;Boleto 10;;;;;;;;;;;;;;;;;;
;;Sempre Bom Supermercado ;Av José Abdumassih n 1680;Shoping Park;Fabia;99202-9541;Boleto 10;;;;;;;;;;;;;;;;;;
;;Wancicleia De Jesus Monteiro;R Ivete Cordeiro Da Silva n 1079;Shoping Park;Wancicleia;99940-3286;avista;;;;;;;;;;;;;;;;;;
1643;;Emporio Suzigan;Av Argemiro Evangelista Ferreira n 43;Shopping Park;Sergio;991669540;Boleto 10;;;;;;;;;;;;;;;;;;
1653;;JC Silva Supermecados ;rua professora Maria celia Cence 95;Shopping Park;Marco tulio;;;;;;;;;;;;;;;;;;;;
;;Mana Supermercado Eurelli;Av Boulanger Fonseca n 279;Shopping Park;Gustavo ;99651-0289;;;;;;;;;;;;;;;;;;;
;;Minas Park Supermercado Ltda;R Professora Erostildes Silva De Meneses n 260;Shopping Park;Eduardo;99279-6644;Boleto 10;;;;;;;;;;;;;;;;;;
1707;;Riber Pao Panificadora ;Av Jose Abdumassih n 1217;Shopping Park;;;;;;;;;;;;;;;;;;;;;
1835;;Super Terra Sacolão e Mercearia;av das Moedas 400;Shopping Park;;;;;;;;;;;;;;;;;;;;;
;;Carioca Alimento E Panificação LTDA;Rua Da Carioca n 1095;Tabajaras;Bruna;99201-8590;Boleto 21;;;;;;;;;;;;;;;;;;
;;Carioca Alimento E Panificação LTDA;Rua Da Carioca n 1095;Tabajaras;Bruna;99201-8590;Boleto 21;;;;;;;;;;;;;;;;;;
;;Comercio De Alimento Goianeiro LTDA;R Leblom n 20;Tabajaras;Tiago ;99671-5009;Boleto 10 ;;;;;;;;;;;;;;;;;;
1599;;Araujo e lellis ;av Joao Naves de Avila 1331;Tibery;;;;;;;;;;;;;;;;;;;;;
1815;;Fast Food Amareto ;Av Joao naves de Avila 1331;Tibery;;;;;;;;;;;;;;;;;;;;;
1650;;INDI INDUSTRIA EDISTRIBUIDORA LTDA ;AV: EUROPA 1227;Tibery;Guilherme;99263-1725;Boleto 10;;;;;;;;;;;;;;;;;;
1821;;Risoto e Massa comercio de alimentos ;Av joao Naves de Avila 1331;Tibery;;;;;;;;;;;;;;;;;;;;;
1706;; Rabelo Panificação Ltda;Av Portugal n 595;Tibery ;Bita ;99971-4710;Boleto 14;;;;;;;;;;;;;;;;;;
1598;;Alimentos Karina LTDA;Av Portugal n 588;Tibery ;Alexandra;99107-7530;Boleto 10;;;;;;;;;;;;;;;;;;
;;Carioca Alimento E Panificação LTDA;Av Belgica n 1220;Tibery ;Bruna;99201-8590;Boleto 28;;;;;;;;;;;;;;;;;;
1612;;Casa Pains LTDA ME;Av Portugal n 261;Tibery ;Cid;3213-4773;Boleto 10;;;;;;;;;;;;;;;;;;
;;Indy Pães Panificadora Confeitaria Ltda;Av Espanha n 1260;Tibery ;Kayky;99798-5858;Boleto 10;;;;;;;;;;;;;;;;;;
;;Luismar Ferreira Gomes ME;Av Australia n 1381;Tibery ;Luismar;3213-4734;Boleto 10;;;;;;;;;;;;;;;;;;
1705;;Rabelo & Borba Panificadora E Restaurante ;Av Benjamim Magalhães n 783;Tibery ;Weliton;99747-1943;Boleto 10;;;;;;;;;;;;;;;;;;
;;Romildo Pereira DE Araujo ME;Av Australia n 1053;Tibery ;Romildo;3213-4597;Boleto 10;;;;;;;;;;;;;;;;;;
;;Antonio Jose Moreira Silva Me ;Av Silvio Rugani n 1221;Tubalina;Simone;99313-2680;Boleto 10;;;;;;;;;;;;;;;;;;
;;Vilmar Fernandes ME;Av Silvio Rugani n 373;Tubalina;Isac;99685-3071;Boleto 10;;;;;;;;;;;;;;;;;;
1645;;Freitas Marques Varejo Ltda Me;Av Brasil n 4477;Umuarama ;Andre ;3232-5413;Boleto 10;;;;;;;;;;;;;;;;;;
1817;;Tenda Do Café ;Av Para n 1720;Umuarama ;Isabella;99148-6662;Boleto 10;;;;;;;;;;;;;;;;;;
1635;;Emporio Duque De Caxias LTDA ME;Rua Duque De Caxias n 1582;Vigilato Pereira ;Daniel;99898-1849;Boleto 10;;;;;;;;;;;;;;;;;;
"""
}
