package dk.kb.retro;

import org.jwat.tools.core.CommandLine;
import org.jwat.tools.core.Task;

public class Retro {

	public static final int A_FILES = 1;
	public static final int A_WORKERS = 2;

	public static void main(String[] args) {
		Retro retro = new Retro();
		retro.Main( args );
	}

	public void Main(String[] args) {
		CommandLine.Arguments arguments = null;
		CommandLine cmdLine = new CommandLine();
		cmdLine.addOption( "-w=", A_WORKERS );
		cmdLine.addListArgument( "files", A_FILES, 1, Integer.MAX_VALUE );
		try {
			arguments = cmdLine.parse( args );
			/*
			for ( int i=0; i<arguments.switchArgsList.size(); ++i) {
				argument = arguments.switchArgsList.get( i );
				System.out.println( argument.argDef.id + "," + argument.argDef.subId + "=" + argument.value );
			}
			*/
		}
		catch (CommandLine.ParseException e) {
			System.out.println( getClass().getName() + ": " + e.getMessage() );
			System.exit( 1 );
		}
		if ( arguments == null ) {
			System.out.println( "Retro v0.1.0" );
			System.out.println( "usage: Retro [-w] [file ...]" );
			System.out.println( " -w<x>  thread(s)" );
		}
		else {
			//String path = "kb-pligtsystem-";
			Task task;
			task = new RetroTask();
			task.command(arguments);
		}
	}

}

