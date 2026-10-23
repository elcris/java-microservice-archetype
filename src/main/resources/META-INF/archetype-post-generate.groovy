import groovy.io.FileType

// Archetype filtering uses native line endings. Normalize portable text before
// the first build so the same Spotless checks pass on Windows and Linux.
def project = new File(request.outputDirectory, request.artifactId)
def extensions = ['java', 'xml', 'yaml', 'yml', 'md', 'config', 'properties', 'sql']
def names = ['mvnw', 'Dockerfile', '.gitignore', '.gitattributes', '.dockerignore']
project.eachFileRecurse(FileType.FILES) { file ->
    def extension = file.name.contains('.') ? file.name.tokenize('.').last() : ''
    if (extensions.contains(extension) || names.contains(file.name)) {
        def content = file.getText('UTF-8')
        content = content.replace('\r\n', '\n').replace('\r', '\n')
        if (extension == 'java') {
            // The package parameter can change alphabetical import order.
            def matcher = content =~ /(?m)^import [^\n]+;\n/
            def imports = []
            int first = -1
            int last = -1
            while (matcher.find()) {
                if (first == -1) first = matcher.start()
                last = matcher.end()
                imports.add(matcher.group().trim())
            }
            if (!imports.isEmpty()) {
                def statics = imports.findAll { it.startsWith('import static ') }.sort()
                def ordinary = imports.findAll { !it.startsWith('import static ') }.sort()
                def groups = [statics, ordinary].findAll { !it.isEmpty() }
                def block = groups.collect { it.join('\n') }.join('\n\n') + '\n\n'
                content = content.substring(0, first) + block + content.substring(last).replaceFirst(/^\n+/, '')
            }
        }
        file.setText(content, 'UTF-8')
    }
}
