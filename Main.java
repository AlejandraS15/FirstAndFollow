import java.util.*;

public class Main {

    public static List<String> getProductions(String prod) {
        List<String> derivations = new ArrayList<>();
        StringBuilder dev = new StringBuilder();
        for (int i = 0; i < prod.length(); i++) {
            if (prod.charAt(i) == ' ') {
                if (dev.length() > 0) {
                    derivations.add(dev.toString());
                    dev = new StringBuilder();
                }
            } else {
                dev.append(prod.charAt(i));
            }
        }
        if (dev.length() > 0) {
            derivations.add(dev.toString());
        }
        return derivations;
    }

    public static Gramatica getGrammar(int gLines, Scanner sc) {
        Gramatica gramatica = new Gramatica();
        while (gLines-- > 0) {
            String production = sc.nextLine();
            char head = production.charAt(0);
            List<String> prods = getProductions(production.substring(2));
            gramatica.order.add(head);
            gramatica.gramatica.put(head, prods);
        }
        return gramatica;
    }

    public static boolean findInFirst(List<Character> first, char terminal) {
        return first.contains(terminal);
    }

    public static void mergeFirsts(Map<Character, Properties> properties, char head, char value) {
        for (char terminal : properties.get(value).firstSet) {
            if (!findInFirst(properties.get(head).firstSet, terminal) && terminal != 'e') {
                properties.get(head).firstSet.add(terminal);
            }
        }
    }

    public static void findFirst(Map<Character, Properties> properties, Gramatica gramatica, char nonTerminal) {
        List<Character> first = properties.get(nonTerminal).firstSet;
        boolean hasNext = false;

        for (String dev : gramatica.gramatica.get(nonTerminal)) {
            int i = 0;
            char value = dev.charAt(i);
            if (value == nonTerminal) {
                if (i + 1 < dev.length()) hasNext = true;
                continue;
            }

            if (Character.isUpperCase(value)) {
                if (properties.get(value).firstSet.isEmpty()) {
                    findFirst(properties, gramatica, value);
                }
                mergeFirsts(properties, nonTerminal, value);
                i++;
                while (properties.get(value).hasEpsilon && i < dev.length()) {
                    value = dev.charAt(i);
                    if (!Character.isUpperCase(value)) {
                        if (!findInFirst(first, value)) first.add(value);
                        break;
                    } else {
                        if (properties.get(value).firstSet.isEmpty()) {
                            findFirst(properties, gramatica, value);
                        }
                        mergeFirsts(properties, nonTerminal, value);
                        i++;
                    }
                    if (i == dev.length() && properties.get(value).hasEpsilon) {
                        first.add('e');
                        properties.get(nonTerminal).hasEpsilon = true;
                    }
                }
            } else {
                if (!findInFirst(first, value)) first.add(value);
                if (value == 'e') properties.get(nonTerminal).hasEpsilon = true;
            }
        }

        if (properties.get(nonTerminal).hasEpsilon && hasNext) {
            for (String dev : gramatica.gramatica.get(nonTerminal)) {
                if (dev.charAt(0) == nonTerminal && dev.length() > 1) {
                    int i = 1;
                    char next = dev.charAt(i);
                    if (Character.isUpperCase(next)) {
                        if (properties.get(next).firstSet.isEmpty()) findFirst(properties, gramatica, next);
                        mergeFirsts(properties, nonTerminal, next);
                        while (properties.get(next).hasEpsilon && i < dev.length()) {
                            i++;
                            next = dev.charAt(i);
                            if (Character.isUpperCase(next)) {
                                if (properties.get(next).firstSet.isEmpty()) findFirst(properties, gramatica, next);
                                mergeFirsts(properties, nonTerminal, next);
                            } else {
                                if (!findInFirst(first, next)) first.add(next);
                                break;
                            }
                        }
                    } else {
                        if (!findInFirst(first, next)) first.add(next);
                    }
                }
            }
        }
    }

    public static void getFirsts(Map<Character, Properties> properties, Gramatica gramatica) {
        for (char head : gramatica.order) {
            if (properties.get(head).firstSet.isEmpty()) {
                findFirst(properties, gramatica, head);
            }
        }
    }

    public static int findInProduction(String production, char value) {
        return production.indexOf(value);
    }

    public static void addCharToFollow(List<Character> follow, char terminal) {
        if (!follow.contains(terminal)) {
            follow.add(terminal);
        }
    }

    public static void addFirstToFollow(Map<Character, Properties> properties, char nonTerminal, char head) {
        for (char terminal : properties.get(head).firstSet) {
            if (terminal != 'e') addCharToFollow(properties.get(nonTerminal).followSet, terminal);
        }
    }

    public static void mergeFollows(List<Character> followB, List<Character> followA) {
        for (char inFollowA : followA) {
            if (!followB.contains(inFollowA)) {
                followB.add(inFollowA);
            }
        }
    }

    public static void findFollow(Map<Character, Properties> properties, Gramatica gramatica, char nonTerminal) {
        List<Character> follow = properties.get(nonTerminal).followSet;
        for (char head : gramatica.order) {
            for (String dev : gramatica.gramatica.get(head)) {
                int pos = findInProduction(dev, nonTerminal);
                if (pos >= 0) {
                    if (pos + 1 < dev.length()) {
                        char val = dev.charAt(pos + 1);
                        if (Character.isUpperCase(val)) {
                            addFirstToFollow(properties, nonTerminal, val);
                            int i = 2;
                            while (properties.get(val).hasEpsilon && pos + i < dev.length()) {
                                val = dev.charAt(pos + i);
                                i++;
                                if (Character.isUpperCase(val)) {
                                    addFirstToFollow(properties, nonTerminal, val);
                                } else {
                                    addCharToFollow(follow, val);
                                    break;
                                }
                            }
                            if (pos + i == dev.length() && properties.get(val).hasEpsilon) {
                                if (properties.get(val).followSet.isEmpty()) {
                                    findFollow(properties, gramatica, head);
                                }
                                mergeFollows(follow, properties.get(head).followSet);
                            }
                        } else {
                            addCharToFollow(follow, val);
                        }
                    } else {
                        if (properties.get(head).followSet.isEmpty()) {
                            findFollow(properties, gramatica, head);
                        }
                        mergeFollows(follow, properties.get(head).followSet);
                    }
                }
            }
        }
    }

    public static void getFollows(Map<Character, Properties> properties, Gramatica gramatica) {
        properties.get('S').followSet.add('$');
        findFollow(properties, gramatica, 'S');
        for (char head : gramatica.order) {
            if (properties.get(head).followSet.isEmpty()) {
                findFollow(properties, gramatica, head);
            }
        }
    }

    public static void printFirsts(Map<Character, Properties> properties, Gramatica gramatica) {
        for (char nonTerminal : gramatica.order) {
            System.out.print("First(" + nonTerminal + ") = {");
            boolean first = true;
            for (char val : properties.get(nonTerminal).firstSet) {
                if (!first) {
                    System.out.print(", ");
                }
                System.out.print(val);
                first = false;
            }
            System.out.println("}");
        }
    }

    public static void printFollows(Map<Character, Properties> properties, Gramatica gramatica) {
        for (char nonTerminal : gramatica.order) {
            System.out.print("Follow(" + nonTerminal + ") = {");
            boolean first = true;
            for (char val : properties.get(nonTerminal).followSet) {
                if (!first) {
                    System.out.print(", ");
                }
                System.out.print(val);
                first = false;
            }
            System.out.println("}");
        }
    }

    // kkkk
    public static void topDown(Map<Character, Properties> properties, Gramatica gramatica) {
        // Mapa para registrar las combinaciones [NoTerminal, Terminal] ya vistas
        Map<Character, Set<Character>> seenCombinations = new HashMap<>();
        boolean isLL1 = true; // Suponemos que la gramática es LL(1) inicialmente

        for (char nonTerminal : gramatica.order) {  // Itera sobre cada no terminal en la gramática
            List<String> producciones = gramatica.gramatica.get(nonTerminal);

            for (String produccion : producciones) {  // Itera sobre cada producción del no terminal
                Set<Character> firstSetForProduction = new HashSet<>();  // Almacena los símbolos First de esta producción

                // Encuentra el conjunto First de esta producción
                for (char symbol : produccion.toCharArray()) {
                    if (Character.isUpperCase(symbol)) {  // Es un no terminal
                        firstSetForProduction.addAll(properties.get(symbol).firstSet);
                        // Si el símbolo contiene épsilon, seguimos analizando el siguiente símbolo
                        if (!properties.get(symbol).hasEpsilon) break;

                    } else {  // Es un terminal
                        if (Character.isLowerCase(symbol)) {
                            firstSetForProduction.add(symbol);
                        }
                        break;
                    }
                }

                // Añadimos el conjunto Follow en caso de épsilon
                if (firstSetForProduction.contains('e')) {
                    firstSetForProduction.addAll(properties.get(nonTerminal).followSet);
                    firstSetForProduction.remove('e');
                }

                // Imprimir en el formato [No terminal, First[i]] = Producción y verificar LL(1)
                for (char firstSymbol : firstSetForProduction) {
                    System.out.println("[" + nonTerminal + ", " + firstSymbol + "] = " + produccion);

                    // Verificar si ya hemos visto esta combinación [NoTerminal, Terminal]
                    seenCombinations.putIfAbsent(nonTerminal, new HashSet<>()); // Inicializa el set si no existe

                    if (seenCombinations.get(nonTerminal).contains(firstSymbol)) {
                        // Si la combinación ya existe, no es LL(1)
                        isLL1 = false;
                    } else {
                        // Agregar la combinación al conjunto visto
                        seenCombinations.get(nonTerminal).add(firstSymbol);
                    }
                }
            }
        }

        // Imprimir si la gramática es LL(1) o no
        if (isLL1) {
            System.out.println("La gramática es LL(1).");
        } else {
            System.out.println("La gramática NO es LL(1).");
        }
    }



    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int cases = sc.nextInt();
        while (cases-- > 0) {
            int gLines = sc.nextInt(); // Número de líneas de la gramática.
            sc.nextLine(); // Consumir el salto de línea.
            Gramatica gramatica = getGrammar(gLines, sc);
            Map<Character, Properties> properties = new HashMap<>();
            for (char head : gramatica.order) {
                properties.put(head, new Properties());
            }
            getFirsts(properties, gramatica);
            getFollows(properties, gramatica);
            printFirsts(properties, gramatica);
            printFollows(properties, gramatica);
            topDown(properties, gramatica);
        }
        sc.close();
    }
}
