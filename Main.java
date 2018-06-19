class Main {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java Main \"Realm\" \"Guild name\"");
			System.exit(1);
		} else {
			Fetcher fetcher = new Fetcher(args[0], args[1]);
			fetcher.run();
			System.exit(0);
		}
	}
}