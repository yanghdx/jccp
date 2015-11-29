package com.venustech.jccp.doclibs.core.interceptor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.i18n.I18n;
import com.jfinal.i18n.Res;
import com.venustech.jccp.doclibs.core.WebConst;
import com.venustech.jccp.doclibs.core.exception.BusinessException;
import com.venustech.jccp.doclibs.util.HttpHelper;

/**
 * 全局异常拦截器
 * @author  yang_huidi  
 * 
 */
public class ExceptionInterceptor implements Interceptor {

	
	private static final Logger LOGGER = 
			Logger.getLogger(ExceptionInterceptor.class);
			
	public void intercept(Invocation inv) {
		final Controller controller = inv.getController();
		final HttpServletRequest request = controller.getRequest();
		try {
			inv.invoke();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage(), e);
			String msg = this.getExceptionMsg(e);
			if (HttpHelper.isAjax(request)) {
				final Map<String, String> error = new HashMap<String, String>(8);
				error.put(WebConst.ReturnConst.RESULT, WebConst.ReturnConst.ERROR);
				error.put(WebConst.ReturnConst.MSG, msg);
            	controller.renderJson(error);
			} else {
				controller.setAttr(WebConst.ReturnConst.RESULT, WebConst.ReturnConst.ERROR);
				controller.setAttr(WebConst.ReturnConst.MSG, msg);
				controller.renderText(msg);
			}
		} 
	}
	
	
	/**
	 * 从异常中获取信息
	 * @author yang_huidi
	 * @param ex
	 * @return
	 */
	private String getExceptionMsg(Exception ex) {
		Res res = I18n.use();
		if (ex == null) {
			return res.get("commonError");
		} else if (ex instanceof BusinessException) {
			return ex.getMessage();
		} else if (ex instanceof IllegalArgumentException) {
			return res.get("calcError");
		} else if (ex instanceof SQLException) {
			return res.get("dbAccessError");
		} else if (ex instanceof IOException) {
			return res.get("ioError");
		} else if (ex instanceof ArithmeticException) {
			return res.get("calcError");
		} 
		//else if (ex instanceof MaxUploadSizeExceededException) {
		//	errmsg = "上传文件大小超过限制";
		//} 
		else if (ex instanceof FileNotFoundException ) {
			return res.get("fileNotFoundError");
		} else if (ex instanceof ArrayIndexOutOfBoundsException ) {
			return res.get("indexError");
		} else if (ex instanceof SecurityException) {
			return res.get("securityError");
		} else {
			return res.get("commonError");
		}
		
	}

}
