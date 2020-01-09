package org.jahia.modules.external.modules.servlet;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.modules.ModulesDataSource;
import org.jahia.services.content.JCRSessionFactory;
import org.osgi.service.component.annotations.Component;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URLEncoder;

@Component(service = {HttpServlet.class, Servlet.class}, property = {"alias=/filesource", "osgi.http.whiteboard.servlet.asyncSupported=true"}, immediate = true)
public class ModulesSourcesFileServlet extends HttpServlet implements Servlet {
    private static Logger logger = LoggerFactory.getLogger(ModulesSourcesFileServlet.class);
    private static final String DEFAULT_ENCODING = "UTF-8";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filePath = request.getParameter("filepath");
        String providerPathWIthVersion = request.getParameter("providerpath");

        if (filePath == null || providerPathWIthVersion == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters for file and provider paths");
        }

        try {
            ModulesDataSource mds = ((ModulesDataSource) ((ExternalContentStoreProvider) JCRSessionFactory.getInstance().getProvider(providerPathWIthVersion)).getDataSource());
            String[] propValues = mds.getPropertyValues(filePath, ModulesDataSource.SOURCE_CODE);
            response.setCharacterEncoding(DEFAULT_ENCODING);
            response.setContentType(getMimetype(filePath, propValues[0]));
            response.setHeader("Content-disposition", "filename=" + URLEncoder.encode(StringUtils.substringAfterLast(filePath, "/"), DEFAULT_ENCODING));
            IOUtils.write(propValues[0], response.getOutputStream());
        } catch (PathNotFoundException e) {
            logger.error("File not found", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not retrieve file");
        }
    }

    private String getMimetype(String fileName, String content) throws IOException {
        String mimetype = URLConnection.guessContentTypeFromName(fileName);

        if (mimetype == null) {
            mimetype = URLConnection.guessContentTypeFromStream(IOUtils.toInputStream(content));
        }

        return mimetype;
    }
}
