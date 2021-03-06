package com.venustech.jccp.doclibs.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.i18n.I18n;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.CacheName;
import com.jfinal.plugin.ehcache.EvictInterceptor;
import com.jfinal.plugin.ehcache.IDataLoader;
import com.jfinal.upload.UploadFile;
import com.venustech.jccp.doclibs.controller.interceptor.AdminMenuInterceptor;
import com.venustech.jccp.doclibs.controller.validator.DocAddValidator;
import com.venustech.jccp.doclibs.controller.validator.DocEditValidator;
import com.venustech.jccp.doclibs.core.WebConst;
import com.venustech.jccp.doclibs.core.online.OnlineUser;
import com.venustech.jccp.doclibs.model.Admin;
import com.venustech.jccp.doclibs.model.Doc;
import com.venustech.jccp.doclibs.model.DocType;
import com.venustech.jccp.doclibs.model.Menu;
import com.venustech.jccp.doclibs.service.AdminService;
import com.venustech.jccp.doclibs.service.DocService;
import com.venustech.jccp.doclibs.service.MenuService;
import com.venustech.jccp.doclibs.util.DateHelper;
import com.venustech.jccp.doclibs.util.ZipUtil;

/**
 * 后台管理员controller
 * @author yanghdx
 *
 */
@Before({AdminMenuInterceptor.class})
public class AdminController extends Controller {

	private static final Logger LOGGER = Logger.getLogger(AdminController.class);
	
	//docService
	private DocService docService = enhance(DocService.class);
	
	//adminService
	private AdminService adminService = enhance(AdminService.class);
	
	/**
	 * admin管理首页
	 */
	public void index() {
		render("admin-index.html");
	}
	
	/**
	 * admin查看doc列表s
	 */
	public void docs() {
		setAttr("menus", CacheKit.get(WebConst.CacheKey.MENUS, "menuList", new IDataLoader() {
			public Object load() {
				//加载普通menu
				return (new MenuService()).loadMenus();
			}
		}));
		render("admin-docs.html");
	}
	
	/**
	 * 添加doc页面
	 */
	public void docAdd() {
		createToken("addToken");
		setAttr("menus", CacheKit.get(WebConst.CacheKey.MENUS, "menuList", new IDataLoader() {
			public Object load() {
				//加载普通menu
				return (new MenuService()).loadMenus();
			}
		}));
		render("doc-add.html");
	}
	
	
	public void docEdit() {
		int docId = getParaToInt();
		Doc doc = docService.getById(docId);
		if (doc == null) {
			renderText("Doc not exist");
			return;
		} else {
			createToken("editToken");
			setAttr("menus", CacheKit.get(WebConst.CacheKey.MENUS, "menuList", new IDataLoader() {
				public Object load() {
					//加载普通menu
					return (new MenuService()).loadMenus();
				}
			}));
			setAttr("doc", doc);
			render("doc-edit.html");
		}
		
	}
	
	
	/**
	 * 添加doc
	 */
	@Before(DocAddValidator.class)
	public void doDocAdd() {
		final UploadFile file = getFile("docFile");
		Doc doc = getModel(Doc.class);
		String[] type = getPara("type").split("-");
		doc.set("upload_time", DateHelper.now())
			.set("doc_path", file.getFileName())
			.set("visible", 1)
			.set("menu_id", type[0])
			.set("type_id", type[1])
			.set("html_view", "on".equals(getPara("htmlview")) ? 1 : 0);
		boolean result = docService.save(doc);
		if (result) {
			if (file.getFileName().toLowerCase().endsWith(".zip")) {
				String unzip = getPara("unzip");
				String htmlview = getPara("htmlview");
				if ("on".equals(unzip)) {
					final String zipFileName = file.getFileName();
					new Thread() {
						public void run() {
							try {
								long start = System.currentTimeMillis();
								unzip(zipFileName);
								long cost = (System.currentTimeMillis() - start)/1000;
								LOGGER.info("Unzip [" + zipFileName + "], cost " + cost + "s!");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}.start();
				}
				System.out.println(unzip + " - " + htmlview);
			}
			redirect("/admin/docAdd?result=success");
		} else {
			setAttr("errmsg", I18n.use().get("admin.add.doc.error"));
			render("doc-add.html");
		}
	}
	
	@Before(DocEditValidator.class)
	public void doDocEdit() {
		String[] type = getPara("type").split("-");
		
		Doc doc = docService.getById(this.getParaToInt("doc.id"));
		doc.set("doc_name", this.getPara("doc.doc_name"));
		doc.set("menu_id", type[0]).set("type_id", type[1]);
		
		final UploadFile file = getFile("docFile");
		if (file != null) {
			String docPath = PathKit.getWebRootPath() + 
					File.separator + WebConst.Upload.PATH + 
					File.separator + doc.getStr("doc_path");
			File oldFile = new File(docPath);
			if (oldFile.exists()) {
				oldFile.delete();
			}
			doc.set("doc_path", file.getFileName())
			   .set("html_view", "on".equals(getPara("htmlview")) ? 1 : 0);
			if (file.getFileName().toLowerCase().endsWith(".zip")) {
				String unzip = getPara("unzip");
				String htmlview = getPara("htmlview");
				if ("on".equals(unzip)) {
					final String zipFileName = file.getFileName();
					new Thread() {
						public void run() {
							try {
								long start = System.currentTimeMillis();
								unzip(zipFileName);
								long cost = (System.currentTimeMillis() - start)/1000;
								LOGGER.info("Unzip [" + zipFileName + "], cost " + cost + "s!");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}.start();
				}
				System.out.println(unzip + " - " + htmlview);
			}
		} 
		docService.update(doc);
		redirect("/admin/docs");
	}
	
	/**
	 * 修改密码页面
	 */
	public void pwdMod() {
		createToken("modToken");
		render("pwd-mod.html");
	}
	
	public void doPwdMod() {
		if (!validateToken("modToken")) {
			setAttr("errmsg", I18n.use().get("common.token.error"));
			createToken("modToken");
			render("pwd-mod.html");
			return;
		}
		String oldPwd = getPara("oldPwd");//md5(md5(md5(passwords) + user) + token)
		if (StrKit.isBlank(oldPwd)) {
			setAttr("errmsg", I18n.use().get("admin.oldpwd.error"));
			createToken("modToken");
			render("pwd-mod.html");
			return;
		}
		String newPwd = getPara("newPwd"); //md5(md5(passwords) + user)
		if (StrKit.isBlank(newPwd) || !newPwd.equals(getPara("rNewPwd"))) {
			setAttr("errmsg", I18n.use().get("admin.newpwd.error"));
			createToken("modToken");
			render("pwd-mod.html");
			return;
		}
		OnlineUser onlineUser = CacheKit.get(WebConst.CacheKey.ONLINE_USERS, getSession().getId());
		Admin admin = adminService.getById(onlineUser.getUserId());
		String dbPwd = admin.getStr("pass");
		if (!oldPwd.equals(HashKit.md5(dbPwd + getPara("modToken")))) {
			setAttr("errmsg", I18n.use().get("admin.oldpwd.error"));
			createToken("modToken");
			render("pwd-mod.html");
		} else {
			adminService.update(admin.set("pass", newPwd));
			redirect("/admin/pwdMod?result=success");
		}
	}
	
	private static void unzip(String fileName) {
		String uploadPath = PathKit.getWebRootPath() + File.separator + WebConst.Upload.PATH + File.separator;
		String zipFilePath = uploadPath + fileName;
		ZipUtil.unZip(zipFilePath, uploadPath);
	}
	
	public void delDoc() {
		int docId = getParaToInt();
		Doc doc = docService.getById(docId);
		if (doc != null) {
			String docPath = PathKit.getWebRootPath() + 
					File.separator + WebConst.Upload.PATH + 
					File.separator + doc.getStr("doc_path");
			File file = new File(docPath);
			if (file.exists()) {
				file.delete();
			}
			docService.deleteById(docId);
		}
		redirect("/admin/docs");
	}
	
	
	
	//menu service
	private MenuService menuService = enhance(MenuService.class);
	
	public void menus() {
		setAttr("menus", CacheKit.get(WebConst.CacheKey.MENUS, "menuList", new IDataLoader() {
			public Object load() {
				//加载普通menu
				return (new MenuService()).loadMenus();
			}
		}));
		render("menu-list.html");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void addDocType() {
		DocType docType = getModel(DocType.class);
		menuService.addDocType(getParaToInt("menuId"), docType);
		renderJson("{\"result\":\"success\", \"id\" : \""+docType.getInt("id")+"\"}");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void modDocType() {
		DocType docType = menuService.getDocTypeById(getParaToInt("typeId"));
		docType.set("type_name", getPara("typeName"));
		menuService.updateDocType(docType);
		renderJson("{\"result\":\"success\"}");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void delDocType() {
		int docTypeId = getParaToInt();
		menuService.deleteDocTypeById(docTypeId);
		renderJson("{\"result\":\"success\"}");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void addMenu() {
		Menu menu = new Menu();
		menu.set("menu_name", getPara("menuName"))
			.set("visible", 1)
			.set("parent_id", 0)
			.set("menu_type", 0)
			.set("func_url", "");
		menuService.add(menu);
		renderJson("{\"result\":\"success\", \"id\":\""+menu.getInt("id")+"\"}");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void modMenu() {
		Menu menu = menuService.getById(getParaToInt("menuId"));
		menu.set("menu_name", getPara("menuName"));
		menuService.update(menu);
		renderJson("{\"result\":\"success\"}");
	}
	
	@Before(EvictInterceptor.class)
	@CacheName(WebConst.CacheKey.MENUS)
	public void delMenu() {
		int menuId = getParaToInt();
		Map<String, String> result = new HashMap<String, String>();
		if (menuService.hasChildren(menuId)) {
			result.put("result", "error");
			result.put("msg", I18n.use().get("menu.has.children"));
		} else {
			menuService.deleteById(menuId);
			result.put("result", "success");
		}
		renderJson(result);
	}
	
	public void about() {
		render("about.html");
	}
}
