package com.alogic.remote.xscript;

import com.alogic.remote.Response;
import com.alogic.xscript.AbstractLogiclet;
import com.alogic.xscript.ExecuteWatcher;
import com.alogic.xscript.Logiclet;
import com.alogic.xscript.LogicletContext;
import com.alogic.xscript.doc.XsObject;
import com.anysoft.util.BaseException;
import com.anysoft.util.Properties;
import com.anysoft.util.PropertiesConstants;

/**
 * Request处理器父类
 * 
 * @author yyduan
 * @since 1.6.10.3
 */
public abstract class ResponseHandler extends AbstractLogiclet{

	protected String pid = "remote-res";
	
	public ResponseHandler(String tag, Logiclet p) {
		super(tag, p);
	}

	@Override
	public void configure(Properties p){
		super.configure(p);
		
		pid = PropertiesConstants.getString(p,"pid",pid,true);
	}
	
	@Override
	protected void onExecute(final XsObject root,final XsObject current,final LogicletContext ctx,final ExecuteWatcher watcher){
		Response req = ctx.getObject(pid);
		if (req == null){
			throw new BaseException("core.no_response","It must be in a remote-call context,check your script.");
		}
		
		onExecute(req,root,current,ctx,watcher);
	}

	protected abstract void onExecute(final Response res,
			final XsObject root,final XsObject current, final LogicletContext ctx,
			final ExecuteWatcher watcher);

}