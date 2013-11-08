JNDI Tomcat Session Manager
============================

Overview
--------

This is based on the excellent Mongo-Tomcat-Sessions code.  I needed something more generic to a standard RDMS.  This will use a defined JNDI database connection to read/write session data to the database.

Usage
-----

Extend DAO with your required database implementation.  I have Oracle and H2 included.  

Modify the context or server file to add the following:

		<Valve className="edu.uow.tomcat.DBSessionTrackerValve" />
		<Manager className="edu.uow.tomcat.DBManager" 
			jndi="jdbc/myDbConnection" 
			DAOClass="edu.uow.tomcat.OracleDAO" 
			maxInactiveInterval="600"/> 
			
The Valve must be before the Manager.

The following parameters are available on the Manager :-

<table>
<tr><td>maxInactiveInterval</td><td>The initial maximum time interval, in seconds, between client requests before a session is invalidated. </td></tr>
<tr><td>jndi</td><td>The defined jndi connection to use</td></tr>
<tr><td>DAOClass</td><td>The implementation class</td></tr>
</table>

Drop the jdbc jar in the Tomcat lib along with this built jar then make sure the table structure has been created. 

