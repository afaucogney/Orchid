package com.eden.orchid.impl.compilers.jtwig;

import com.eden.common.util.EdenUtils;
import com.eden.orchid.api.compilers.OrchidCompiler;
import com.eden.orchid.api.registration.AutoRegister;
import com.eden.orchid.api.registration.OrchidRegistrationProvider;
import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.environment.EnvironmentFactory;
import org.jtwig.functions.JtwigFunction;
import org.jtwig.resource.loader.TypedResourceLoader;
import org.jtwig.resource.reference.ResourceReference;

import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class JTwigCompiler implements OrchidCompiler, OrchidRegistrationProvider {

    public EnvironmentConfigurationBuilder config = EnvironmentConfigurationBuilder.configuration();

    @Override
    public String compile(String extension, String source, Object... data) {
        String s = "";
        if(data != null && data.length > 0 && data[0] != null) {
            s = data[0].toString();
        }

        JtwigModel model = null;

        if(!EdenUtils.isEmpty(s)) {
            model = JtwigModel.newModel(new JSONObject(s).toMap());
        }
        else {
            model = JtwigModel.newModel();
        }

        return new JtwigTemplate(
                new EnvironmentFactory().create(config.build()),
                new ResourceReference(ResourceReference.STRING, source)
        ).render(model);
    }

    @Override
    public String getOutputExtension() {
        return "html";
    }

    @Override
    public String[] getSourceExtensions() {
        return new String[] {"html", "twig"};
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public void register(Object object) {
        if(object instanceof JtwigFunction) {
            config.functions().add((JtwigFunction) object);
        }

        if(object instanceof TypedResourceLoader) {
            List<TypedResourceLoader> loaders = new ArrayList<>(config.resources().resourceLoaders().build());
            loaders.add(0, (TypedResourceLoader) object);
            config.resources().resourceLoaders().set(loaders);
        }
    }
}