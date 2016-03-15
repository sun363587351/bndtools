package org.bndtools.templating.jgit;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.service.reporter.Reporter;

@Component(name = "org.bndtools.core.templating.workspace", property = {
        Constants.SERVICE_DESCRIPTION + "=Load workspace templates from configured Git repositories"
})
public class GitHubWorkspaceTemplateLoader implements TemplateLoader {

    private static final String TEMPLATE_TYPE = "workspace";

    private final Cache cache = new Cache();

    @Override
    public List<Template> findTemplates(String type, Reporter reporter) {
        if (!TEMPLATE_TYPE.equals(type))
            return Collections.emptyList();

        List<Template> templates = new LinkedList<>();

        Parameters githubRepos = new GitRepoPreferences().getGithubRepos();
        for (Entry<String,Attrs> entry : githubRepos.entrySet()) {
            String repo = entry.getKey();
            Attrs attribs = entry.getValue();

            try {
                GitHub gitHub = new GitHub(cache);
                GithubRepoDetailsDTO detailsDTO = gitHub.loadRepoDetails(repo);
                if (detailsDTO.clone_url == null)
                    throw new IllegalArgumentException("Missing clone URL");
                URI cloneUri = URI.create(detailsDTO.clone_url);

                // Generate icon URI from the owner avatar. The s=16 parameter
                // is added to select a 16x16 icon.
                URI avatarUri = null;
                if (detailsDTO.owner.avatar_url != null)
                    avatarUri = URI.create(detailsDTO.owner.avatar_url + "&s=16");

                String name = attribs.get("name");
                if (name == null)
                    name = repo;
                GitCloneTemplate template = new GitCloneTemplate(cloneUri, name, detailsDTO.description, "GitHub", avatarUri);
                templates.add(template);
            } catch (Exception e) {
                reporter.exception(e, "Error loading template from Github repository %s", repo);
            }
        }

        return templates;
    }

}