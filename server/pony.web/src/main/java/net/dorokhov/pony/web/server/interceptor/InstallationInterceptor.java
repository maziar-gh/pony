package net.dorokhov.pony.web.server.interceptor;

import net.dorokhov.pony.web.server.controller.ApiController;
import net.dorokhov.pony.web.server.controller.InstallationController;
import net.dorokhov.pony.web.server.service.InstallationServiceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InstallationInterceptor extends HandlerInterceptorAdapter {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private InstallationServiceFacade installationServiceFacade;

	@Autowired
	public void setInstallationServiceFacade(InstallationServiceFacade aInstallationService) {
		installationServiceFacade = aInstallationService;
	}

	@Override
	public boolean preHandle(HttpServletRequest aRequest, HttpServletResponse aResponse, Object aHandler) throws Exception {

		if (aHandler instanceof HandlerMethod) {

			HandlerMethod handlerMethod = (HandlerMethod)aHandler;

			boolean isInstallationController = (handlerMethod.getBean() instanceof InstallationController);
			boolean isInstallationRequest = (handlerMethod.getBean() instanceof ApiController && handlerMethod.getMethod().getName().equals("getInstallation"));

			if (!isInstallationController && !isInstallationRequest) {
				if (installationServiceFacade.getInstallation() == null) {

					log.info("Redirecting to installation...");

					aResponse.sendRedirect(aRequest.getContextPath() + "/install");

					return false;
				}
			}
		}

		return true;
	}

}