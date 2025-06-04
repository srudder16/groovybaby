// Simple MagicDraw acronym extractor - extracts all acronyms without definitions
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Extract potential acronyms from text using regex patterns
 */
def extractAcronymsFromText(String text) {
    if (!text) return []

    // Pattern for acronyms: 2+ uppercase letters, possibly with numbers
    Pattern acronymPattern = Pattern.compile("\\b[A-Z]{2,}(?:\\d+)?\\b")
    Matcher matcher = acronymPattern.matcher(text)

    List<String> acronyms = []
    while (matcher.find()) {
        acronyms.add(matcher.group())
    }

    // Filter out very common non-acronyms (optional - you can remove this section if you want everything)
    Set<String> excludeWords = ['AND', 'OR', 'NOT', 'ALL', 'XML', 'JSON', 'HTTP', 'URL', 'API', 'UML']
    List<String> filteredAcronyms = acronyms.findAll { !excludeWords.contains(it) }

    return filteredAcronyms.unique()
}

/**
 * Extract acronyms from MagicDraw model elements
 */
def extractFromMagicDrawModel() {
    Map<String, List<String>> acronyms = [
            'model_info': [],
            'elements': [],
            'diagrams': [],
            'packages': [],
            'stereotypes': []
    ]

    try {
        // Get the current project
        def project = com.nomagic.magicdraw.core.Application.getInstance().getProject()
        if (!project) {
            println "No project is currently open in MagicDraw"
            return acronyms
        }

        def model = project.getModel()

        // Extract from model name
        if (model.getName()) {
            acronyms['model_info'].addAll(extractAcronymsFromText(model.getName()))
        }

        // Get all elements in the model
        def allElements = com.nomagic.magicdraw.core.Application.getInstance()
                .getProject().getElementsOfType(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element.class, true)

        allElements.each { element ->
            // Extract from element names
            if (element.getName()) {
                acronyms['elements'].addAll(extractAcronymsFromText(element.getName()))
            }

            // Extract from element documentation
            if (element.getDocumentation()) {
                acronyms['elements'].addAll(extractAcronymsFromText(element.getDocumentation()))
            }

            // Extract from stereotypes
            element.getAppliedStereotype().each { stereotype ->
                if (stereotype.getName()) {
                    acronyms['stereotypes'].addAll(extractAcronymsFromText(stereotype.getName()))
                }
            }
        }

        // Get all diagrams
        def diagrams = com.nomagic.magicdraw.core.Application.getInstance()
                .getProject().getDiagrams()

        diagrams.each { diagram ->
            if (diagram.getName()) {
                acronyms['diagrams'].addAll(extractAcronymsFromText(diagram.getName()))
            }
            if (diagram.getDocumentation()) {
                acronyms['diagrams'].addAll(extractAcronymsFromText(diagram.getDocumentation()))
            }
        }

        // Get all packages
        def packages = com.nomagic.magicdraw.core.Application.getInstance()
                .getProject().getElementsOfType(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package.class, true)

        packages.each { pkg ->
            if (pkg.getName()) {
                acronyms['packages'].addAll(extractAcronymsFromText(pkg.getName()))
            }
            if (pkg.getDocumentation()) {
                acronyms['packages'].addAll(extractAcronymsFromText(pkg.getDocumentation()))
            }
        }

        // Remove duplicates from each category
        acronyms.each { category, list ->
            acronyms[category] = list.unique()
        }

    } catch (Exception e) {
        println "Error extracting from MagicDraw model: ${e.message}"
        e.printStackTrace()
    }

    return acronyms
}

/**
 * Extract acronyms from text file
 */
def extractFromTextFile(String filePath) {
    Map<String, List<String>> acronyms = [
            'file_content': []
    ]

    try {
        File file = new File(filePath)
        if (!file.exists()) {
            println "File not found: ${filePath}"
            return acronyms
        }

        String content = file.text
        acronyms['file_content'] = extractAcronymsFromText(content)

    } catch (Exception e) {
        println "Error reading file: ${e.message}"
    }

    return acronyms
}

/**
 * Simple XML parsing without XmlSlurper (basic string matching)
 */
def extractFromXmlFile(String filePath) {
    Map<String, List<String>> acronyms = [
            'xml_attributes': [],
            'xml_content': []
    ]

    try {
        File file = new File(filePath)
        if (!file.exists()) {
            println "File not found: ${filePath}"
            return acronyms
        }

        String content = file.text

        // Extract from XML attributes (id, name, etc.)
        Pattern attrPattern = Pattern.compile('(?:id|name)=["\'](.*?)["\']')
        Matcher attrMatcher = attrPattern.matcher(content)
        while (attrMatcher.find()) {
            acronyms['xml_attributes'].addAll(extractAcronymsFromText(attrMatcher.group(1)))
        }

        // Extract from XML text content
        Pattern contentPattern = Pattern.compile('>(.*?)<')
        Matcher contentMatcher = contentPattern.matcher(content)
        while (contentMatcher.find()) {
            String textContent = contentMatcher.group(1).trim()
            if (textContent && !textContent.startsWith('<')) {
                acronyms['xml_content'].addAll(extractAcronymsFromText(textContent))
            }
        }

        // Remove duplicates
        acronyms.each { category, list ->
            acronyms[category] = list.unique()
        }

    } catch (Exception e) {
        println "Error parsing XML file: ${e.message}"
    }

    return acronyms
}

/**
 * Display extracted acronyms in a formatted way
 */
def displayAcronyms(Map<String, List<String>> acronymsDict) {
    println "=== ACRONYMS FOUND ===\n"

    acronymsDict.each { category, acronymList ->
        if (acronymList && !acronymList.isEmpty()) {
            println "${category.toUpperCase().replace('_', ' ')}:"
            acronymList.sort().each { acronym ->
                println "  • ${acronym}"
            }
            println()
        }
    }

    // Summary statistics
    int totalAcronyms = acronymsDict.values().sum { it.size() } ?: 0
    println "Total unique acronyms found: ${totalAcronyms}"

    // Create a flat list of all unique acronyms
    List<String> allUniqueAcronyms = []
    acronymsDict.values().each { categoryAcronyms ->
        allUniqueAcronyms.addAll(categoryAcronyms)
    }
    allUniqueAcronyms = allUniqueAcronyms.unique().sort()

    if (!allUniqueAcronyms.isEmpty()) {
        println "\nAll unique acronyms: ${allUniqueAcronyms.join(', ')}"
    }
}

/**
 * Main function - analyze current MagicDraw model
 */
def analyzeMagicDrawModel() {
    println "=== MAGICDRAW MODEL ACRONYM EXTRACTOR ===\n"

    def acronyms = extractFromMagicDrawModel()
    displayAcronyms(acronyms)

    return acronyms
}

/**
 * Analyze external file
 */
def analyzeFile(String filePath) {
    println "=== FILE ACRONYM EXTRACTOR ===\n"
    println "Analyzing file: ${filePath}\n"

    Map<String, List<String>> acronyms

    if (filePath.toLowerCase().endsWith('.xml') || filePath.toLowerCase().endsWith('.sbml')) {
        acronyms = extractFromXmlFile(filePath)
    } else {
        acronyms = extractFromTextFile(filePath)
    }

    displayAcronyms(acronyms)

    return acronyms
}

/**
 * Test function with sample text
 */
def testAcronymExtraction() {
    println "=== TEST ACRONYM EXTRACTION ===\n"

    String sampleText = "This system uses REST API with JSON format, connecting to SQL database via HTTPS protocol."
    List<String> testAcronyms = extractAcronymsFromText(sampleText)

    println "Sample text: ${sampleText}"
    println "Extracted acronyms: ${testAcronyms}"
    println()
}

// Execute the main analysis
try {
    // Test the extraction first
    testAcronymExtraction()

    println "\n" + "="*50 + "\n"

    // Try to analyze the current MagicDraw model
    analyzeMagicDrawModel()

} catch (Exception e) {
    println "Error during execution: ${e.message}"
    e.printStackTrace()

    // Fallback: just run the test
    println "\nRunning test extraction only:"
    testAcronymExtraction()
}

// Usage instructions
println "\n=== USAGE INSTRUCTIONS ===\n"
println "// To analyze current MagicDraw model:"
println "// analyzeMagicDrawModel()"
println "//"
println "// To analyze an external file:"
println "// analyzeFile('/path/to/your/file.xml')"
println "//"
println "// To extract from custom text:"
println "// extractAcronymsFromText('Your text with REST API and JSON')"