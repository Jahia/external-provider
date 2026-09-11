// Puts a real gzip file inside the mounted folder, so a root layering the gzip provider over a local file
// resolves. Without the file such a root fails for lack of one, which says nothing about whether the scheme
// itself is allowed.
import java.util.zip.GZIPOutputStream

def archive = new File('#path#')
new GZIPOutputStream(new FileOutputStream(archive)).withCloseable { gzos ->
    gzos.write('inside'.getBytes('UTF-8'))
}
setResult(archive.exists())
