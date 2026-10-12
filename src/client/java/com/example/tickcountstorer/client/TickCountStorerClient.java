package com.example.tickcountstorer.client;

import net.fabricmc.api.ClientModInitializer;

public class TickCountStorerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TickCountStorage.register();
	}
}