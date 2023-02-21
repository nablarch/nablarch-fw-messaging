package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * フレームワークのテスト用に{@link ServletContext}を生成するクラス。
 */
public class MockServletContextCreator implements ServletContextCreator{

    @Override
    public ServletContext create(HttpServletRequest request) {
        return request.getSession()
                .getServletContext();
    }
}
