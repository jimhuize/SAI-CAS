/*
	   Copyright (C) 2005-2006 Sergey Koposov
   
    Author: Sergey Koposov
    Email: math@sai.msu.ru 
    http://lnfm1.sai.msu.ru/~math

    This file is part of SAI CAS

    SAI CAS is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    SAI CAS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SAI CAS; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/



package sai_cas.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.io.File;

import java.util.List;
import java.util.Calendar;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.*;
import org.apache.commons.fileupload.disk.*;

import sai_cas.VOTABLEFile.VOTABLE;
import sai_cas.VOTABLEFile.Votable;
import sai_cas.VOTABLEFile.VotableException;
import sai_cas.db.*;
import sai_cas.output.CSVQueryResultsOutputter;
import sai_cas.output.QueryResultsOutputter;
import sai_cas.output.VOTableQueryResultsOutputter;
import sai_cas.vo.*;


public class UploadServlet extends HttpServlet {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("sai_cas.UploadServlet");
	public class UploadServletException extends Exception
	{
		UploadServletException()
		{
			super();
		}
		UploadServletException(String s)
		{
			super(s);
		}

	}
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, java.io.IOException
	{

		PrintWriter out = response.getWriter();
		String user = null, password = null;
		String catalogString = null, formatString = null;
		File uploadedFile = null;
		Connection conn = null;
		DBInterface dbi = null;
		
		List<FileItem> fileItemList = null;

		FileItemFactory factory = new DiskFileItemFactory();
		try 
		{		
			ServletFileUpload sfu = new ServletFileUpload(factory);
			sfu.setSizeMax(50000000);
			/* Request size <= 50Mb */
			fileItemList = sfu.parseRequest(request);
			
		}
		catch (FileUploadException e)
		{
			throw new ServletException(e.getMessage()); 
			/* Nothing ...*/
		}
		
		FileItem fi = null;
		
		for (FileItem fi0: fileItemList)
		{
			if (fi0.getFieldName().equals("file"))//(!fi0.isFormField())
			{
				fi = fi0;
			}
			if (fi0.getFieldName().equals("user"))//(!fi0.isFormField())
			{
				user = fi0.getString();
			}
			if (fi0.getFieldName().equals("password"))//(!fi0.isFormField())
			{
				password = fi0.getString();
			}
/*			if (fi0.getFieldName().equals("rad"))//(!fi0.isFormField())
			{
				radString = fi0.getString();
			}
			if (fi0.getFieldName().equals("racol"))//(!fi0.isFormField())
			{
				raColumn = fi0.getString();
			}
			if (fi0.getFieldName().equals("deccol"))//(!fi0.isFormField())
			{
				decColumn = fi0.getString();
			}
*/
			if (fi0.getFieldName().equals("format"))//(!fi0.isFormField())
			{
				formatString = fi0.getString();
			}
		}
		try
		{
			Votable vot;

			if (fi == null)
			{
				throw new ServletException("File must be specified" + fileItemList.size() );			
			}
			if (user == null)
			{
				throw new ServletException("User name must be specified" + fileItemList.size() );			
			}
			if (password == null)
			{
				throw new ServletException("Password must be specified" + fileItemList.size() );			
			}

			long size = fi.getSize();

			if (size > 10000000) 
			{
				throw new UploadServletException("File is too big");
			}
			if (size == 0) 
			{
				throw new UploadServletException("File must not be empty");
			}
			uploadedFile = File.createTempFile("crossmatch",".dat",new File("/tmp/"));
			try
			{
				fi.write(uploadedFile);
			}
			catch (Exception e)
			{
				throw new UploadServletException("Error in writing your data in the temporary file");
			}
			
			logger.debug("File written");
			
			try
			{
				conn = DBConnection.getPooledPerUserConnection(user, password);
				dbi = new DBInterface(conn, user);
				String userDataSchema = dbi.getUserDataSchemaName();
				vot = new Votable(uploadedFile);
				vot.insertDataToDB(dbi,userDataSchema);
			}
			catch (SQLException e)
			{
					logger.error("Got an exception... ", e);
					throw new UploadServletException(e.getMessage());			
			}
			catch (VotableException e)
			{
					logger.error("Got an Votable exception... ", e);
					throw new UploadServletException(e.getMessage());
			}
			catch (DBException e)
			{
				logger.error("Got an DB exception... ", e);
				throw new UploadServletException(e.getMessage());
			}
			DBInterface.close(dbi, conn);
			out.println("Success");
		}
		catch (UploadServletException e)
		{
			out.println("Upload failed: " + e.getMessage());
			DBInterface.close(dbi,conn,false);
		}
		finally 
		{
			try 
			{
				uploadedFile.delete();
			}
			catch (Exception e)
			{
			}
		}
	}
}
