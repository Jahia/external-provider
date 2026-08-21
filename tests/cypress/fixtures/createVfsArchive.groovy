// Puts a real archive inside the mounted folder, so a root layering the zip/jar provider over a
// local file resolves. Without the archive such a root fails for lack of a file, which says
// nothing about whether the scheme itself is allowed.
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

def archive = new File('#path#')
new ZipOutputStream(new FileOutputStream(archive)).withCloseable { zos ->
    zos.putNextEntry(new ZipEntry('inside.txt'))
    zos.write('inside'.getBytes('UTF-8'))
    zos.closeEntry()
}
setResult(archive.exists())
