/** ========================================================================= *
 * Copyright (C) 2011,2012  IBM Corporation ( http://www.ibm.com/ )           *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package biz.taoconsulting.dominodav.methods;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class handles all unimplemented methods with a nice error screen Doesn't set
 * the response code since it could be called from different status
 * 
 * @author Stephan H. Wissel
 * 
 */
public class Unimplemented extends AbstractDAVMethod {

	private String ErrorMessage = null;
	private String errNum;

	public void setErrNum(String errNum) {
		this.errNum = errNum;
	}

	public void setErrNum(int errNum) {
		this.errNum = String.valueOf(errNum);
	}

	/**
	 * Sets the error message to call - if there is one
	 * 
	 * @param errorMessage
	 */
	public void setErrorMessage(String errorMessage) {
		ErrorMessage = errorMessage;
	}

	@Override
	protected void action() throws Exception {
		HttpServletResponse res = this.getResp();
		HttpServletRequest req = this.getReq();
		String method = req.getMethod();

		PrintWriter pw = this.getWriter(res);
		pw.write("<html>\n<head>\n<title>");
		pw.write("Call to webDAV servlet failed on " + method);
		pw.write("</title>\n<style type=\"text/css\">\n");
		pw.write("table {width : 100%}\n");
		pw.write("th {border-bottom : 2px solid red}\n");
		pw.write("td {border-right : 1px solid gray; margin : 0; padding 3px; border-bottom : 1px solid gray;}\n");
		pw.write(".label {background-color : #FAFAFA}\n");
		pw.write(".value {background-color : #FFFAFA}\n}");
		pw.write("</style>\n</head>\n<body>\n");

		pw.write("<h1>");
		pw.write(this.errNum);
		pw.write(": ");
		pw.write((this.ErrorMessage == null) ? "Error when calling " + method
				: this.ErrorMessage);

		pw.write("</h1>\n");
		pw.write("<table>");
		pw.write("<tr><th colspan=\"2\"><h2>Parameters</h2></th></tr>");

		@SuppressWarnings("rawtypes")
		Map p = req.getParameterMap();
		for (Object k : p.keySet()) {
			pw.write("<tr><td class=\"label\">");
			pw.write(k.toString());
			pw.write("</td><td class=\"value\">");
			pw.write(p.get(k).toString());
			pw.write("</td></tr>");
		}

		pw.write("<tr><th colspan=\"2\"><h2>Headers</h2></th></tr>");
		@SuppressWarnings("rawtypes")
		Enumeration headerNames = req.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			String curHeader = headerNames.nextElement().toString();
			pw.write("<tr><td class=\"label\">");
			pw.write(curHeader);
			pw.write("</td><td class=\"value\">");
			pw.write(req.getHeader(curHeader));
			pw.write("</td></tr>");
		}

		pw.write("<tr><th colspan=\"2\"><h2>Cookies</h2></th></tr>");
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				pw.write("<tr><td class=\"label\">");
				pw.write(cookies[i].getName());
				pw.write("</td><td class=\"value\">");
				pw.write(cookies[i].getValue());
				pw.write("</td></tr>");
			}
		}
		pw.write("</table>");
		pw.write("\n</body>\n</html>");
		pw.close();
	}

	/**
	 * In a servlet we can use only one the writer or the output stream. Here we
	 * try to get one
	 * 
	 * @param res
	 * @return
	 * @throws IOException
	 */
	private PrintWriter getWriter(HttpServletResponse res) throws IOException {

		PrintWriter result = null;

		if (this.streamUsed()) {
			OutputStream out = res.getOutputStream();
			result = new PrintWriter(out);
		} else {
			result = res.getWriter();
		}

		return result;
	}

	@Override
	protected void writeInitialHeader() {
		// No header needed

	}

	/**
	 * Tell the Error mwthod not to use the PrintWriter
	 * 
	 * @param streamUsed
	 */
	public void setUseStream(boolean streamUsed) {
		this.streamHasBeenUsed();

	}

}
