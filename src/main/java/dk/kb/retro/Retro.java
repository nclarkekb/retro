package dk.kb.retro;

import org.jwat.tools.JWATTools;

import dk.kb.retro.tasks.index.IndexTask;
import dk.kb.retro.tasks.retro.RetroTask;

public class Retro extends JWATTools {

	public static void main(String[] args) {
		Retro retro = new Retro();
		retro.Main( args );
	}

	@Override
	public void configure_cli() {
		super.configure_cli();
		commands.put("retro", RetroTask.class);
		commands.put("index", IndexTask.class);
	}

	@Override
	public void show_help() {
		super.show_help();
		System.out.println( "Retro v0.1.0" );
		System.out.println( "usage: Retro [-w] [file ...]" );
		System.out.println( "" );
		System.out.println( "Commands:" );
		System.out.println( "   retro        create pligt html pages");
		System.out.println( "   index        create servlet index data");
		System.out.println( "" );
		System.out.println( " -w<x> thread(s)" );
	}

}
