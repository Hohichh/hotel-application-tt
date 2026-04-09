package gp.app.hotels.exception;

import java.util.UUID;

public class HotelNotFoundException extends AppException {
    private final UUID hotelId;

    public HotelNotFoundException(UUID hotelId) {
        super("Hotel not found with id: " + hotelId);
        this.hotelId = hotelId;
    }

    public UUID getHotelId() {
        return hotelId;
    }
}
